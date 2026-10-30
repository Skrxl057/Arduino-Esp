package com.embedhub.app.core.transport

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
import java.net.InetSocketAddress
import java.net.Socket

class TcpLink(
    private val host: String,
    private val port: Int,
    override val name: String = "TCP ($host:$port)"
) : Link {

    override val type: LinkType = LinkType.TCP
    override val mtu: Int = 1400

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _incomingLines = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingLines: SharedFlow<String> = _incomingLines.asSharedFlow()

    private val _incomingBytes = MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingBytes: SharedFlow<ByteArray> = _incomingBytes.asSharedFlow()

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var readJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun open(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _state.value = ConnectionState.Connecting
            val sock = Socket()
            sock.connect(InetSocketAddress(host, port), 5000)
            socket = sock
            outputStream = sock.getOutputStream()
            inputStream = sock.getInputStream()

            _state.value = ConnectionState.Connected(name, "$host:$port")
            startReader()
            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = ConnectionState.Error(e.message ?: "TCP connection failed", e)
            close()
            Result.failure(e)
        }
    }

    private fun startReader() {
        readJob?.cancel()
        readJob = scope.launch {
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
        readJob?.cancel()
        readJob = null
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
