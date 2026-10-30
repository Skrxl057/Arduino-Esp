package com.embedhub.app.core.model

import kotlinx.serialization.Serializable

enum class LinkType {
    BLE,
    CLASSIC_BT,
    USB_SERIAL,
    TCP,
    UDP,
    WEBSOCKET,
    HTTP,
    MQTT
}

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val deviceName: String, val addressOrUri: String) : ConnectionState()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int, val nextAttemptMs: Long) : ConnectionState()
    data class Error(val message: String, val cause: Throwable? = null) : ConnectionState()
}

@Serializable
enum class LineEnding(val chars: String) {
    NONE(""),
    LF("\n"),
    CR("\r"),
    CRLF("\r\n")
}

@Serializable
enum class ChecksumType {
    NONE,
    XOR,
    CRC8
}

@Serializable
data class ConnectionConfig(
    val linkType: LinkType = LinkType.BLE,
    val address: String = "",
    val port: Int = 80,
    val baudRate: Int = 115200,
    val dataBits: Int = 8,
    val stopBits: Int = 1,
    val parity: Int = 0, // 0: None, 1: Odd, 2: Even
    val dtrRts: Boolean = true,
    // MQTT specifics
    val mqttClientId: String = "EmbedHubClient",
    val mqttUsername: String = "",
    val mqttPassword: String = "",
    val mqttSubTopics: List<String> = listOf("esp32/telemetry", "robot/status"),
    val mqttPubTopic: String = "esp32/cmd",
    val mqttQos: Int = 1,
    val mqttRetain: Boolean = false,
    val mqttUseTls: Boolean = false,
    // HTTP specifics
    val httpUrlTemplate: String = "http://{host}:{port}/?State={cmd}",
    // Reconnect & Backup Transport
    val autoReconnect: Boolean = true,
    val maxReconnectAttempts: Int = 5,
    val fallbackLinkType: LinkType? = null,
    val fallbackAddress: String = "",
    // Framing & Checksum
    val checksumType: ChecksumType = ChecksumType.NONE,
    val frameStart: String = "",
    val frameEnd: String = "",
    val sendRateLimitHz: Int = 20
)

data class DeviceSlot(
    val slotIndex: Int, // 0, 1, 2 (Up to 3 devices concurrently)
    val name: String,
    val colorHex: String,
    val config: ConnectionConfig,
    val state: ConnectionState = ConnectionState.Disconnected,
    val txBytes: Long = 0,
    val rxBytes: Long = 0,
    val errorCount: Long = 0,
    val lastLatencyMs: Long = 0
)

data class LogMessage(
    val id: String,
    val timestamp: Long,
    val isTx: Boolean,
    val text: String,
    val rawBytes: ByteArray,
    val slotIndex: Int
) {
    val hexString: String
        get() = rawBytes.joinToString(" ") { "%02X".format(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as LogMessage
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

@Serializable
data class MacroButton(
    val id: String,
    val label: String,
    val command: String,
    val lineEnding: LineEnding = LineEnding.CRLF,
    val isHex: Boolean = false,
    val repeatIntervalMs: Long = 0L // 0 = one shot
)

@Serializable
enum class ScreenOrientationMode {
    AUTO_SENSOR,       // Follows device accelerometer / system orientation
    LOCKED_PORTRAIT,   // Locked to portrait mode
    LOCKED_LANDSCAPE,  // Locked to landscape mode (ideal for robot car / drone driving)
    REVERSE_LANDSCAPE  // Inverted landscape for charging cable access
}

@Serializable
data class ControlProfile(
    val forwardCmd: String = "F",
    val backwardCmd: String = "B",
    val leftCmd: String = "L",
    val rightCmd: String = "R",
    val stopCmd: String = "S",
    val joystickRateHz: Int = 20,
    val joystickDeadzone: Float = 0.08f,
    val joystickExpo: Float = 1.0f, // 1.0 = linear, >1 = exponential sensitivity
    val joystickFormat: String = "X:{x},Y:{y}", // e.g., X:-100..100,Y:-100..100
    val continuousSpeed: Boolean = false,
    val preferredOrientation: ScreenOrientationMode = ScreenOrientationMode.AUTO_SENSOR,
    val customButtons: List<CustomControlButton> = listOf(
        CustomControlButton("btn_horn", "Horn", "H", "h", isToggle = false),
        CustomControlButton("btn_light", "Headlights", "W", "w", isToggle = true),
        CustomControlButton("btn_aux", "Aux Relay", "A1", "A0", isToggle = true)
    )
)

@Serializable
data class CustomControlButton(
    val id: String,
    val label: String,
    val pressCmd: String,
    val releaseCmd: String = "",
    val isToggle: Boolean = false,
    val isToggledOn: Boolean = false
)

@Serializable
enum class WidgetType {
    GAUGE,
    LINE_GRAPH,
    VALUE_TILE,
    LED_INDICATOR,
    SWITCH,
    SLIDER,
    BUTTON,
    TEXT_DISPLAY,
    MULTI_CHART
}

@Serializable
data class DashboardWidget(
    val id: String,
    val title: String,
    val type: WidgetType,
    val dataKey: String,
    val targetSlot: Int = 0,
    val min: Double = 0.0,
    val max: Double = 100.0,
    val unit: String = "",
    val colorHex: String = "#0284C7",
    val commandTemplate: String = "",
    val lastValue: Double = 0.0,
    val lastText: String = ""
)

@Serializable
data class AutomationRule(
    val id: String,
    val name: String,
    val isEnabled: Boolean = true,
    val cooldownMs: Long = 1000L,
    val lastExecutedAt: Long = 0L,
    val trigger: RuleTrigger,
    val condition: RuleCondition? = null,
    val actions: List<RuleAction> = emptyList()
)

@Serializable
sealed class RuleTrigger {
    @Serializable
    data class DataReceived(val key: String, val operator: String, val threshold: Double) : RuleTrigger()
    @Serializable
    data class DataChanged(val key: String) : RuleTrigger()
    @Serializable
    data class IntervalSchedule(val intervalSeconds: Int) : RuleTrigger()
    @Serializable
    data class ConnectionEvent(val targetSlot: Int, val isConnected: Boolean) : RuleTrigger()
    @Serializable
    data class PhoneEvent(val eventType: String) : RuleTrigger() // BATTERY_LOW, SHAKE, CHARGING
}

@Serializable
sealed class RuleCondition {
    @Serializable
    data class Compare(val key: String, val operator: String, val value: Double) : RuleCondition()
    @Serializable
    data class And(val conditions: List<RuleCondition>) : RuleCondition()
    @Serializable
    data class Or(val conditions: List<RuleCondition>) : RuleCondition()
}

@Serializable
sealed class RuleAction {
    @Serializable
    data class SendCommand(val targetSlot: Int, val command: String) : RuleAction()
    @Serializable
    data class ShowNotification(val title: String, val body: String) : RuleAction()
    @Serializable
    data class HapticVibrate(val durationMs: Long) : RuleAction()
    @Serializable
    data class TextToSpeech(val text: String) : RuleAction()
    @Serializable
    data class Delay(val delayMs: Long) : RuleAction()
}
