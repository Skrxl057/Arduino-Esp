package com.embedhub.app.core.transport

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.embedhub.app.core.model.ConnectionState
import com.embedhub.app.core.model.LinkType
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class UsbSerialLink(
    private val context: Context,
    private val usbDevice: UsbDevice,
    private val baudRate: Int = 115200,
    private val dataBits: Int = 8,
    private val stopBits: Int = UsbSerialPort.STOPBITS_1,
    private val parity: Int = UsbSerialPort.PARITY_NONE,
    private val dtrRts: Boolean = true,
    override val name: String = "USB Serial (${usbDevice.deviceName})"
) : Link {

    override val type: LinkType = LinkType.USB_SERIAL
    override val mtu: Int = 1024

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _incomingLines = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingLines: SharedFlow<String> = _incomingLines.asSharedFlow()

    private val _incomingBytes = MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingBytes: SharedFlow<ByteArray> = _incomingBytes.asSharedFlow()

    private var port: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null
    private val lineBuffer = StringBuilder()

    private val ioListener = object : SerialInputOutputManager.Listener {
        override fun onNewData(data: ByteArray) {
            _incomingBytes.tryEmit(data)
            val text = String(data, Charsets.UTF_8)
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
            }
        }

        override fun onRunError(e: Exception) {
            _state.value = ConnectionState.Error("USB IO error: ${e.message}", e)
        }
    }

    override suspend fun open(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
                ?: return@withContext Result.failure(IllegalStateException("UsbManager unavailable"))

            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            val driver = availableDrivers.firstOrNull { it.device == usbDevice }
                ?: return@withContext Result.failure(IllegalStateException("No USB serial driver found for device"))

            val connection = usbManager.openDevice(driver.device)
                ?: return@withContext Result.failure(IllegalStateException("USB permission denied or device open failed"))

            val serialPort = driver.ports[0]
            serialPort.open(connection)
            serialPort.setParameters(baudRate, dataBits, stopBits, parity)

            if (dtrRts) {
                serialPort.dtr = true
                serialPort.rts = true
            }

            port = serialPort
            ioManager = SerialInputOutputManager(serialPort, ioListener).apply {
                start()
            }

            _state.value = ConnectionState.Connected(name, usbDevice.deviceName)
            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = ConnectionState.Error(e.message ?: "Failed to open USB serial", e)
            close()
            Result.failure(e)
        }
    }

    override suspend fun send(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val p = port ?: return@withContext Result.failure(IllegalStateException("USB port not open"))
        try {
            p.write(bytes, 2000)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun toggleDtrRts(enable: Boolean) {
        try {
            port?.dtr = enable
            port?.rts = enable
        } catch (_: Exception) {}
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        try {
            ioManager?.stop()
            ioManager = null
            port?.close()
            port = null
        } catch (_: Exception) {}
        _state.value = ConnectionState.Disconnected
    }
}
