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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UdpLink(
    private val host: String,
    private val port: Int,
    override val name: String = "UDP ($host:$port)"
) : Link {

    override val type: LinkType = LinkType.UDP
    override val mtu: Int = 1400

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _incomingLines = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingLines: SharedFlow<String> = _incomingLines.asSharedFlow()

    private val _incomingBytes = MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingBytes: SharedFlow<ByteArray> = _incomingBytes.asSharedFlow()

    private var socket: DatagramSocket? = null
    private var inetAddress: InetAddress? = null
    private var listenJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun open(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _state.value = ConnectionState.Connecting
            inetAddress = InetAddress.getByName(host)
            val sock = DatagramSocket()
            socket = sock
            _state.value = ConnectionState.Connected(name, "$host:$port")
            startListener(sock)
            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = ConnectionState.Error(e.message ?: "Failed to open UDP socket", e)
            close()
            Result.failure(e)
        }
    }

    private fun startListener(sock: DatagramSocket) {
        listenJob?.cancel()
        listenJob = scope.launch {
            val buffer = ByteArray(2048)
            while (isActive && !sock.isClosed) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    sock.receive(packet)
                    val received = buffer.copyOf(packet.length)
                    _incomingBytes.tryEmit(received)
                    val text = String(received, Charsets.UTF_8).trim()
                    if (text.isNotEmpty()) {
                        _incomingLines.tryEmit(text)
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    override suspend fun send(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val sock = socket ?: return@withContext Result.failure(IllegalStateException("UDP socket not open"))
        val addr = inetAddress ?: return@withContext Result.failure(IllegalStateException("Target address unresolved"))
        try {
            val packet = DatagramPacket(bytes, bytes.size, addr, port)
            sock.send(packet)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        listenJob?.cancel()
        listenJob = null
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        inetAddress = null
        _state.value = ConnectionState.Disconnected
    }
}
