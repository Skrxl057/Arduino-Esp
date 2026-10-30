package com.embedhub.app.core.provisioning

import com.embedhub.app.core.transport.BleLink
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WifiCredentialsPayload(
    val ssid: String,
    val pass: String,
    val hostname: String = "embedhub-esp"
)

object BleProvisioner {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Formats WiFi credentials as JSON for ESP32 provisioning sketch
     */
    fun createProvisioningPayload(ssid: String, pass: String, hostname: String = "esp32-node"): String {
        val payload = WifiCredentialsPayload(ssid, pass, hostname)
        return json.encodeToString(payload)
    }

    suspend fun provisionOverBle(bleLink: BleLink, ssid: String, pass: String, hostname: String = "esp32-node"): Result<Unit> {
        val payload = createProvisioningPayload(ssid, pass, hostname) + "\n"
        return bleLink.send(payload.toByteArray(Charsets.UTF_8))
    }
}
