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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class HttpLink(
    private val host: String,
    private val port: Int = 80,
    private val urlTemplate: String = "http://{host}:{port}/?State={cmd}",
    override val name: String = "HTTP ($host:$port)"
) : Link {

    override val type: LinkType = LinkType.HTTP
    override val mtu: Int = 2048

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _incomingLines = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingLines: SharedFlow<String> = _incomingLines.asSharedFlow()

    private val _incomingBytes = MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingBytes: SharedFlow<ByteArray> = _incomingBytes.asSharedFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    override suspend fun open(): Result<Unit> = withContext(Dispatchers.IO) {
        _state.value = ConnectionState.Connected(name, "$host:$port")
        Result.success(Unit)
    }

    override suspend fun send(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cmd = String(bytes, Charsets.UTF_8).trim()
            val encodedCmd = URLEncoder.encode(cmd, "UTF-8")
            val targetUrl = urlTemplate
                .replace("{host}", host)
                .replace("{port}", port.toString())
                .replace("{cmd}", encodedCmd)

            val request = Request.Builder().url(targetUrl).build()
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (responseBody.isNotEmpty()) {
                    _incomingBytes.tryEmit(responseBody.toByteArray(Charsets.UTF_8))
                    _incomingLines.tryEmit(responseBody.trim())
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        _state.value = ConnectionState.Disconnected
    }
}
