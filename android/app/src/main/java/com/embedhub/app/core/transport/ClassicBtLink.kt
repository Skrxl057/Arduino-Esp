package com.embedhub.app.core.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
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
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@SuppressLint("MissingPermission")
class ClassicBtLink(
    private val macAddress: String,
    override val name: String = "Classic BT (HC-05/06)"
) : Link {

    override val type: LinkType = LinkType.CLASSIC_BT
    override val mtu: Int = 1024

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _incomingLines = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingLines: SharedFlow<String> = _incomingLines.asSharedFlow()

    private val _incomingBytes = MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingBytes: SharedFlow<ByteArray> = _incomingBytes.asSharedFlow()

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var readerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        // Standard Serial Port Profile (SPP) UUID
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    override suspend fun open(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Result.failure(IllegalStateException("Bluetooth adapter not found"))
            val device = adapter.getRemoteDevice(macAddress)

            _state.value = ConnectionState.Connecting
            adapter.cancelDiscovery() // Always cancel discovery before connecting

            val sock = device.createRfcommSocketToServiceRecord(SPP_UUID)
            sock.connect()

            socket = sock
            outputStream = sock.outputStream
            inputStream = sock.inputStream

            _state.value = ConnectionState.Connected(name, macAddress)
            startReader()
            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = ConnectionState.Error(e.message ?: "Failed to connect SPP", e)
            close()
            Result.failure(e)
        }
    }

    private fun startReader() {
        readerJob?.cancel()
        readerJob = scope.launch {
            val buffer = ByteArray(2048)
            val lineBuilder = StringBuilder()
            while (isActive && socket?.isConnected == true) {
                try {
                    val count = inputStream?.read(buffer) ?: -1
                    if (count <= 0) break
                    val received = buffer.copyOf(count)
                    _incomingBytes.tryEmit(received)

                    val text = String(received, Charsets.UTF_8)
                    lineBuilder.append(text)
                    var newlineIdx = lineBuilder.indexOfAny(charArrayOf('\n', '\r'))
                    while (newlineIdx != -1) {
                        val line = lineBuilder.substring(0, newlineIdx).trim()
                        if (line.isNotEmpty()) {
                            _incomingLines.tryEmit(line)
                        }
                        lineBuilder.delete(0, newlineIdx + 1)
                        newlineIdx = lineBuilder.indexOfAny(charArrayOf('\n', '\r'))
                    }
                } catch (e: Exception) {
                    break
                }
            }
            if (_state.value is ConnectionState.Connected) {
                _state.value = ConnectionState.Disconnected
            }
        }
    }

    override suspend fun send(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val out = outputStream ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            out.write(bytes)
            out.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        readerJob?.cancel()
        readerJob = null
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        outputStream = null
        inputStream = null
        _state.value = ConnectionState.Disconnected
    }
}
