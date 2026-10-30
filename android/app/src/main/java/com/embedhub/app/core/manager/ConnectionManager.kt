package com.embedhub.app.core.manager

import android.content.Context
import com.embedhub.app.core.checksum.ChecksumUtils
import com.embedhub.app.core.model.*
import com.embedhub.app.core.queue.SendQueue
import com.embedhub.app.core.transport.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ConnectionManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    // 3 concurrent device slots
    private val _slots = MutableStateFlow(
        listOf(
            DeviceSlot(0, "Device A (ESP32)", "#0284C7", ConnectionConfig(linkType = LinkType.BLE)),
            DeviceSlot(1, "Device B (Arduino)", "#10B981", ConnectionConfig(linkType = LinkType.CLASSIC_BT)),
            DeviceSlot(2, "Device C (Robot)", "#F59E0B", ConnectionConfig(linkType = LinkType.WEBSOCKET))
        )
    )
    val slots: StateFlow<List<DeviceSlot>> = _slots.asStateFlow()

    private val activeLinks = arrayOfNulls<Link>(3)
    private val sendQueues = arrayOfNulls<SendQueue>(3)
    private val reconnectJobs = arrayOfNulls<Job>(3)

    private val _allIncomingLines = MutableSharedFlow<Pair<Int, String>>(replay = 100)
    val allIncomingLines: SharedFlow<Pair<Int, String>> = _allIncomingLines.asSharedFlow()

    private val _allIncomingBytes = MutableSharedFlow<Pair<Int, ByteArray>>(replay = 100)
    val allIncomingBytes: SharedFlow<Pair<Int, ByteArray>> = _allIncomingBytes.asSharedFlow()

    fun updateSlotConfig(slotIndex: Int, name: String, colorHex: String, config: ConnectionConfig) {
        val current = _slots.value.toMutableList()
        if (slotIndex in current.indices) {
            current[slotIndex] = current[slotIndex].copy(name = name, colorHex = colorHex, config = config)
            _slots.value = current
        }
    }

    suspend fun connect(slotIndex: Int, overrideConfig: ConnectionConfig? = null): Result<Unit> = withContext(Dispatchers.IO) {
        if (slotIndex !in 0..2) return@withContext Result.failure(IllegalArgumentException("Invalid slot"))
        val currentSlot = _slots.value[slotIndex]
        val config = overrideConfig ?: currentSlot.config

        // Disconnect existing if any
        disconnect(slotIndex)

        updateSlotState(slotIndex, ConnectionState.Connecting)

        val link = createLink(config, currentSlot.name)
        activeLinks[slotIndex] = link

        val openResult = link.open()
        if (openResult.isSuccess) {
            // Setup SendQueue
            val queue = SendQueue(
                scope = scope,
                sendBlock = { bytes ->
                    val framed = ChecksumUtils.framePayload(
                        payload = bytes,
                        checksumType = config.checksumType,
                        startMarker = config.frameStart,
                        endMarker = config.frameEnd
                    )
                    val result = link.send(framed)
                    if (result.isSuccess) {
                        recordTx(slotIndex, framed.size.toLong())
                    }
                    result
                },
                mtuSize = link.mtu,
                minIntervalMs = if (config.sendRateLimitHz > 0) (1000L / config.sendRateLimitHz) else 20L
            )
            queue.start()
            sendQueues[slotIndex] = queue

            // Monitor state
            scope.launch {
                link.state.collect { newState ->
                    updateSlotState(slotIndex, newState)
                    if (newState is ConnectionState.Disconnected && config.autoReconnect) {
                        scheduleReconnect(slotIndex, config, attempt = 1)
                    }
                }
            }

            // Monitor lines
            scope.launch {
                link.incomingLines.collect { line ->
                    _allIncomingLines.emit(slotIndex to line)
                }
            }

            // Monitor bytes
            scope.launch {
                link.incomingBytes.collect { bytes ->
                    recordRx(slotIndex, bytes.size.toLong())
                    _allIncomingBytes.emit(slotIndex to bytes)
                }
            }

            Result.success(Unit)
        } else {
            val error = openResult.exceptionOrNull()
            updateSlotState(slotIndex, ConnectionState.Error(error?.message ?: "Failed to open link"))
            if (config.fallbackLinkType != null && config.fallbackAddress.isNotEmpty()) {
                // Try fallback transport
                val fallbackConfig = config.copy(
                    linkType = config.fallbackLinkType,
                    address = config.fallbackAddress,
                    fallbackLinkType = null
                )
                connect(slotIndex, fallbackConfig)
            } else {
                openResult
            }
        }
    }

    private fun scheduleReconnect(slotIndex: Int, config: ConnectionConfig, attempt: Int) {
        if (attempt > config.maxReconnectAttempts) {
            updateSlotState(slotIndex, ConnectionState.Disconnected)
            return
        }

        reconnectJobs[slotIndex]?.cancel()
        reconnectJobs[slotIndex] = scope.launch {
            val backoffMs = (1000L * (1L shl (attempt - 1))).coerceAtMost(16000L)
            updateSlotState(slotIndex, ConnectionState.Reconnecting(attempt, config.maxReconnectAttempts, backoffMs))
            delay(backoffMs)
            val result = connect(slotIndex, config)
            if (result.isFailure) {
                scheduleReconnect(slotIndex, config, attempt + 1)
            }
        }
    }

    suspend fun disconnect(slotIndex: Int) = withContext(Dispatchers.IO) {
        if (slotIndex !in 0..2) return@withContext
        reconnectJobs[slotIndex]?.cancel()
        reconnectJobs[slotIndex] = null

        sendQueues[slotIndex]?.stop()
        sendQueues[slotIndex] = null

        try {
            activeLinks[slotIndex]?.close()
        } catch (_: Exception) {}
        activeLinks[slotIndex] = null
        updateSlotState(slotIndex, ConnectionState.Disconnected)
    }

    suspend fun sendCommand(slotIndex: Int, text: String, lineEnding: LineEnding = LineEnding.CRLF): Result<Unit> {
        val queue = sendQueues[slotIndex] ?: return Result.failure(IllegalStateException("Slot $slotIndex not connected"))
        val fullText = text + lineEnding.chars
        val bytes = fullText.toByteArray(Charsets.UTF_8)
        val success = queue.enqueue(bytes)
        return if (success) Result.success(Unit) else Result.failure(IllegalStateException("Send queue full"))
    }

    suspend fun sendImmediateStop(slotIndex: Int, stopCmd: String = "S", lineEnding: LineEnding = LineEnding.CRLF): Result<Unit> {
        val link = activeLinks[slotIndex] ?: return Result.failure(IllegalStateException("Slot $slotIndex not connected"))
        val bytes = (stopCmd + lineEnding.chars).toByteArray(Charsets.UTF_8)
        return link.send(bytes)
    }

    suspend fun sendEmergencyStopAll(stopCmd: String = "S") {
        for (i in 0..2) {
            try {
                sendImmediateStop(i, stopCmd)
            } catch (_: Exception) {}
        }
    }

    private fun createLink(config: ConnectionConfig, name: String): Link {
        return when (config.linkType) {
            LinkType.BLE -> BleLink(context, config.address, name)
            LinkType.CLASSIC_BT -> ClassicBtLink(config.address, name)
            LinkType.TCP -> TcpLink(config.address, config.port, name)
            LinkType.UDP -> UdpLink(config.address, config.port, name)
            LinkType.WEBSOCKET -> WebSocketLink(config.address, name)
            LinkType.HTTP -> HttpLink(config.address, config.port, config.httpUrlTemplate, name)
            LinkType.MQTT -> MqttLink(
                brokerUri = config.address,
                clientId = config.mqttClientId,
                username = config.mqttUsername,
                password = config.mqttPassword,
                subTopics = config.mqttSubTopics,
                pubTopic = config.mqttPubTopic,
                qos = config.mqttQos,
                retain = config.mqttRetain,
                name = name
            )
            LinkType.USB_SERIAL -> throw UnsupportedOperationException("USB requires UsbDevice reference")
        }
    }

    private fun updateSlotState(slotIndex: Int, state: ConnectionState) {
        val list = _slots.value.toMutableList()
        if (slotIndex in list.indices) {
            list[slotIndex] = list[slotIndex].copy(state = state)
            _slots.value = list
        }
    }

    private fun recordTx(slotIndex: Int, bytes: Long) {
        val list = _slots.value.toMutableList()
        if (slotIndex in list.indices) {
            list[slotIndex] = list[slotIndex].copy(txBytes = list[slotIndex].txBytes + bytes)
            _slots.value = list
        }
    }

    private fun recordRx(slotIndex: Int, bytes: Long) {
        val list = _slots.value.toMutableList()
        if (slotIndex in list.indices) {
            list[slotIndex] = list[slotIndex].copy(rxBytes = list[slotIndex].rxBytes + bytes)
            _slots.value = list
        }
    }

    fun getActiveLink(slotIndex: Int): Link? = activeLinks.getOrNull(slotIndex)
}
