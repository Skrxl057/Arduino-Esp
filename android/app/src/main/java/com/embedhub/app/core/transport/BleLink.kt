package com.embedhub.app.core.transport

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import com.embedhub.app.core.model.ConnectionState
import com.embedhub.app.core.model.LinkType
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

@SuppressLint("MissingPermission")
class BleLink(
    private val context: Context,
    val macAddress: String,
    override val name: String = "BLE Device"
) : Link {

    override val type: LinkType = LinkType.BLE

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _incomingLines = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingLines: SharedFlow<String> = _incomingLines.asSharedFlow()

    private val _incomingBytes = MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingBytes: SharedFlow<ByteArray> = _incomingBytes.asSharedFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var readCharacteristic: BluetoothGattCharacteristic? = null

    private var currentMtu = 23
    override val mtu: Int
        get() = currentMtu - 3

    private val lineBuffer = StringBuilder()
    private var lineFlushJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Known UART UUIDs
    companion object {
        // HM-10 / HM-11
        val HM10_SERVICE: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val HM10_CHAR: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

        // Nordic UART Service (NUS)
        val NORDIC_UART_SERVICE: UUID = UUID.fromString("6e400001-b5a3-f393-a0a9-e50e24dcca9e")
        val NORDIC_RX_CHAR: UUID = UUID.fromString("6e400002-b5a3-f393-a0a9-e50e24dcca9e") // Phone writes here
        val NORDIC_TX_CHAR: UUID = UUID.fromString("6e400003-b5a3-f393-a0a9-e50e24dcca9e") // Phone receives notify here

        // Standard Client Characteristic Configuration Descriptor (CCCD)
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _state.value = ConnectionState.Connected(name, macAddress)
                    // Request MTU 247
                    gatt?.requestMtu(247)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _state.value = ConnectionState.Disconnected
                    cleanup()
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                currentMtu = mtu
            }
            // Discover services after MTU negotiation
            gatt?.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS || gatt == null) return
            autoDetectUartCharacteristics(gatt)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic?.value?.let { data ->
                handleIncomingBytes(data)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleIncomingBytes(value)
        }
    }

    private fun autoDetectUartCharacteristics(gatt: BluetoothGatt) {
        // 1. Check HM-10
        val hm10Service = gatt.getService(HM10_SERVICE)
        if (hm10Service != null) {
            val charac = hm10Service.getCharacteristic(HM10_CHAR)
            if (charac != null) {
                writeCharacteristic = charac
                readCharacteristic = charac
                enableNotification(gatt, charac)
                return
            }
        }

        // 2. Check Nordic UART
        val nusService = gatt.getService(NORDIC_UART_SERVICE)
        if (nusService != null) {
            writeCharacteristic = nusService.getCharacteristic(NORDIC_RX_CHAR)
            readCharacteristic = nusService.getCharacteristic(NORDIC_TX_CHAR)
            readCharacteristic?.let { enableNotification(gatt, it) }
            return
        }

        // 3. Fallback: Search for any service with write + notify characteristics
        for (service in gatt.services) {
            var candidateWrite: BluetoothGattCharacteristic? = null
            var candidateNotify: BluetoothGattCharacteristic? = null

            for (charac in service.characteristics) {
                val props = charac.properties
                if ((props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) ||
                    (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)
                ) {
                    if (candidateWrite == null) candidateWrite = charac
                }
                if ((props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) ||
                    (props and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0)
                ) {
                    if (candidateNotify == null) candidateNotify = charac
                }
            }

            if (candidateWrite != null && candidateNotify != null) {
                writeCharacteristic = candidateWrite
                readCharacteristic = candidateNotify
                enableNotification(gatt, candidateNotify)
                return
            }
        }
    }

    private fun enableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
    }

    private fun handleIncomingBytes(bytes: ByteArray) {
        _incomingBytes.tryEmit(bytes)
        val text = String(bytes, Charsets.UTF_8)
        synchronized(lineBuffer) {
            lineBuffer.append(text)
            var newlineIdx = lineBuffer.indexOfAny(charArrayOf('\n', '\r'))
            while (newlineIdx != -1) {
                val line = lineBuffer.substring(0, newlineIdx).trim()
                if (line.isNotEmpty()) {
                    _incomingLines.tryEmit(line)
                }
                lineBuffer.delete(0, newlineIdx + 1)
                newlineIdx = lineBuffer.indexOfAny(charArrayOf('\n', '\r'))
            }

            // Flush text without a newline after 150 ms
            lineFlushJob?.cancel()
            if (lineBuffer.isNotEmpty()) {
                lineFlushJob = scope.launch {
                    delay(150)
                    synchronized(lineBuffer) {
                        if (lineBuffer.isNotEmpty()) {
                            val line = lineBuffer.toString().trim()
                            if (line.isNotEmpty()) {
                                _incomingLines.tryEmit(line)
                            }
                            lineBuffer.setLength(0)
                        }
                    }
                }
            }
        }
    }

    override suspend fun open(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter ?: return@withContext Result.failure(IllegalStateException("Bluetooth not available"))
            val device = adapter.getRemoteDevice(macAddress)

            _state.value = ConnectionState.Connecting
            bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = ConnectionState.Error(e.message ?: "Failed to connect BLE", e)
            Result.failure(e)
        }
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        cleanup()
    }

    private fun cleanup() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (_: Exception) {}
        bluetoothGatt = null
        writeCharacteristic = null
        readCharacteristic = null
        _state.value = ConnectionState.Disconnected
    }

    override suspend fun send(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val gatt = bluetoothGatt ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        val charac = writeCharacteristic ?: return@withContext Result.failure(IllegalStateException("No write characteristic found"))

        charac.value = bytes
        charac.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        val success = gatt.writeCharacteristic(charac)
        if (success) Result.success(Unit) else Result.failure(IllegalStateException("GATT write failed"))
    }

    /**
     * GATT Explorer support
     */
    fun getDiscoveredServices(): List<BluetoothGattService> {
        return bluetoothGatt?.services ?: emptyList()
    }
}
