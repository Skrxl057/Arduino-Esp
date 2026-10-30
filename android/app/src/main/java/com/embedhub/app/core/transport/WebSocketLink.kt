package com.embedhub.app.core.transport

import com.embedhub.app.core.model.ConnectionState
import com.embedhub.app.core.model.LinkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.ByteString

class WebSocketLink(
    private val wsUrl: String,
    override val name: String = "WebSocket ($wsUrl)"
) : Link {

    override val type: LinkType = LinkType.WEBSOCKET
    override val mtu: Int = 4096

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _incomingLines = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingLines: SharedFlow<String> = _incomingLines.asSharedFlow()

    private val _incomingBytes = MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingBytes: SharedFlow<ByteArray> = _incomingBytes.asSharedFlow()

    private var okHttpClient: OkHttpClient? = null
    private var webSocket: WebSocket? = null

    private val wsListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _state.value = ConnectionState.Connected(name, wsUrl)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            _incomingLines.tryEmit(text.trim())
            _incomingBytes.tryEmit(text.toByteArray(Charsets.UTF_8))
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            val byteArray = bytes.toByteArray()
            _incomingBytes.tryEmit(byteArray)
            _incomingLines.tryEmit(String(byteArray, Charsets.UTF_8).trim())
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
            _state.value = ConnectionState.Disconnected
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _state.value = ConnectionState.Error("WebSocket failure: ${t.message}", t)
        }
    }

    override suspend fun open(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _state.value = ConnectionState.Connecting
            val client = OkHttpClient.Builder().build()
            val request = Request.Builder().url(wsUrl).build()
            webSocket = client.newWebSocket(request, wsListener)
            okHttpClient = client
            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = ConnectionState.Error(e.message ?: "Failed to connect WebSocket", e)
            close()
            Result.failure(e)
        }
    }

    override suspend fun send(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val ws = webSocket ?: return@withContext Result.failure(IllegalStateException("WebSocket not connected"))
        try {
            // Attempt sending as text if valid UTF-8, else binary ByteString
            val text = String(bytes, Charsets.UTF_8)
            val success = ws.send(text)
            if (success) Result.success(Unit) else Result.failure(IllegalStateException("Send failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        try {
            webSocket?.close(1000, "Closed by EmbedHub")
        } catch (_: Exception) {}
        webSocket = null
        okHttpClient = null
        _state.value = ConnectionState.Disconnected
    }
}
