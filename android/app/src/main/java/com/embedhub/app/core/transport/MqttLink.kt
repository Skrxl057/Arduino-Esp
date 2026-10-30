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
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttLink(
    private val brokerUri: String,
    private val clientId: String = "EmbedHubClient",
    private val username: String = "",
    private val password: String = "",
    private val subTopics: List<String> = listOf("esp32/telemetry"),
    private val pubTopic: String = "esp32/cmd",
    private val qos: Int = 1,
    private val retain: Boolean = false,
    override val name: String = "MQTT ($brokerUri)"
) : Link {

    override val type: LinkType = LinkType.MQTT
    override val mtu: Int = 4096

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _incomingLines = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingLines: SharedFlow<String> = _incomingLines.asSharedFlow()

    private val _incomingBytes = MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingBytes: SharedFlow<ByteArray> = _incomingBytes.asSharedFlow()

    private var client: MqttAsyncClient? = null

    override suspend fun open(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _state.value = ConnectionState.Connecting
            val mqttClient = MqttAsyncClient(brokerUri, clientId, MemoryPersistence())
            client = mqttClient

            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 30
                if (username.isNotEmpty()) {
                    userName = username
                    this.password = password.toCharArray()
                }
            }

            mqttClient.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    _state.value = ConnectionState.Connected(name, brokerUri)
                    // Subscribe to configured topics
                    for (topic in subTopics) {
                        try {
                            mqttClient.subscribe(topic, qos)
                        } catch (_: Exception) {}
                    }
                }

                override fun connectionLost(cause: Throwable?) {
                    _state.value = ConnectionState.Disconnected
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val payload = message?.payload ?: return
                    _incomingBytes.tryEmit(payload)
                    val text = String(payload, Charsets.UTF_8).trim()
                    if (text.isNotEmpty()) {
                        _incomingLines.tryEmit("[$topic] $text")
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            val token = mqttClient.connect(options)
            token.waitForCompletion(10000)
            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = ConnectionState.Error(e.message ?: "MQTT connection error", e)
            close()
            Result.failure(e)
        }
    }

    override suspend fun send(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val c = client ?: return@withContext Result.failure(IllegalStateException("MQTT client not initialized"))
        try {
            val message = MqttMessage(bytes).apply {
                this.qos = this@MqttLink.qos
                this.isRetained = this@MqttLink.retain
            }
            c.publish(pubTopic, message)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        try {
            client?.disconnect()
            client?.close()
        } catch (_: Exception) {}
        client = null
        _state.value = ConnectionState.Disconnected
    }
}
