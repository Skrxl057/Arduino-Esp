package com.embedhub.app.ui.screens.connections

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedhub.app.core.manager.ConnectionManager
import com.embedhub.app.core.model.ConnectionConfig
import com.embedhub.app.core.model.ConnectionState
import com.embedhub.app.core.model.LinkType
import com.embedhub.app.core.transport.DiscoveredEsp
import com.embedhub.app.core.transport.MdnsDiscovery
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@Composable
fun ConnectionsScreen(
    connectionManager: ConnectionManager,
    selectedSlot: Int,
    mdnsDiscovery: MdnsDiscovery,
    onNavigateToGattBrowser: (String) -> Unit = {}
) {
    var selectedTransportTab by remember { mutableStateOf(0) }
    val transportTabs = listOf("BLE", "Classic BT", "WiFi / Sockets", "MQTT", "Provisioning")
    val coroutineScope = rememberCoroutineScope()
    val slots by connectionManager.slots.collectAsState()
    val activeSlot = slots[selectedSlot]

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTransportTab) {
            transportTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTransportTab == index,
                    onClick = { selectedTransportTab = index },
                    text = { Text(title, fontSize = 12.sp, maxLines = 1) }
                )
            }
        }

        when (selectedTransportTab) {
            0 -> BleTabContent(connectionManager, selectedSlot, onNavigateToGattBrowser)
            1 -> ClassicBtTabContent(connectionManager, selectedSlot)
            2 -> WifiSocketsTabContent(connectionManager, selectedSlot, mdnsDiscovery)
            3 -> MqttTabContent(connectionManager, selectedSlot)
            4 -> ProvisioningTabContent(connectionManager, selectedSlot)
        }
    }
}

data class ScannedBleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isUartLikely: Boolean
)

@SuppressLint("MissingPermission")
@Composable
fun BleTabContent(
    connectionManager: ConnectionManager,
    selectedSlot: Int,
    onNavigateToGattBrowser: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    val scannedDevices = remember { mutableStateListOf<ScannedBleDevice>() }

    val adapter = remember { BluetoothAdapter.getDefaultAdapter() }
    val scanner = remember(adapter) { adapter?.bluetoothLeScanner }

    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                if (result == null) return
                val dev = result.device
                val name = dev.name ?: "Unknown BLE"
                val addr = dev.address
                val rssi = result.rssi
                val isUart = name.contains("HM", ignoreCase = true) ||
                             name.contains("ESP32", ignoreCase = true) ||
                             name.contains("UART", ignoreCase = true)

                val existing = scannedDevices.indexOfFirst { it.address == addr }
                if (existing != null && existing >= 0) {
                    scannedDevices[existing] = ScannedBleDevice(name, addr, rssi, isUart)
                } else {
                    scannedDevices.add(ScannedBleDevice(name, addr, rssi, isUart))
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BLE Scanner (HM-10, ESP32, Nordic)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = {
                    if (!isScanning) {
                        scannedDevices.clear()
                        scanner?.startScan(scanCallback)
                        isScanning = true
                    } else {
                        scanner?.stopScan(scanCallback)
                        isScanning = false
                    }
                }
            ) {
                Icon(if (isScanning) Icons.Default.Stop else Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isScanning) "Stop Scan" else "Scan BLE")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(scannedDevices) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(device.name, fontWeight = FontWeight.Bold)
                                if (device.isUartLikely) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("UART", fontSize = 10.sp)
                                    }
                                }
                            }
                            Text(device.address, style = MaterialTheme.typography.labelSmall)
                            Text("RSSI: ${device.rssi} dBm", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { onNavigateToGattBrowser(device.address) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("GATT", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        connectionManager.connect(
                                            slotIndex = selectedSlot,
                                            overrideConfig = ConnectionConfig(
                                                linkType = LinkType.BLE,
                                                address = device.address
                                            )
                                        )
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Connect", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun ClassicBtTabContent(
    connectionManager: ConnectionManager,
    selectedSlot: Int
) {
    val coroutineScope = rememberCoroutineScope()
    val adapter = remember { BluetoothAdapter.getDefaultAdapter() }
    val pairedDevices = remember {
        adapter?.bondedDevices?.toList() ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text(
            text = "Classic Bluetooth SPP (HC-05, HC-06, ESP32)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Paired hardware devices (UUID 00001101...)",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(pairedDevices) { dev ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(dev.name ?: "Unknown BT", fontWeight = FontWeight.Bold)
                            Text(dev.address, style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    connectionManager.connect(
                                        slotIndex = selectedSlot,
                                        overrideConfig = ConnectionConfig(
                                            linkType = LinkType.CLASSIC_BT,
                                            address = dev.address
                                        )
                                    )
                                }
                            }
                        ) {
                            Text("Connect")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WifiSocketsTabContent(
    connectionManager: ConnectionManager,
    selectedSlot: Int,
    mdnsDiscovery: MdnsDiscovery
) {
    val coroutineScope = rememberCoroutineScope()
    var host by remember { mutableStateOf("192.168.4.1") }
    var port by remember { mutableStateOf("80") }
    var socketType by remember { mutableStateOf(LinkType.WEBSOCKET) }
    var httpTemplate by remember { mutableStateOf("http://{host}:{port}/?State={cmd}") }

    val discoveredBoards by mdnsDiscovery.discoveredBoards.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("WiFi & Sockets (TCP, UDP, WebSocket, HTTP)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(LinkType.WEBSOCKET, LinkType.TCP, LinkType.UDP, LinkType.HTTP).forEach { type ->
                FilterChip(
                    selected = socketType == type,
                    onClick = { socketType = type },
                    label = { Text(type.name, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Host / IP / URL") },
                modifier = Modifier.weight(2f)
            )
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port") },
                modifier = Modifier.weight(1f)
            )
        }

        if (socketType == LinkType.HTTP) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = httpTemplate,
                onValueChange = { httpTemplate = it },
                label = { Text("HTTP URL Template") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        val parsedPort = port.toIntOrNull() ?: 80
                        val address = if (socketType == LinkType.WEBSOCKET && !host.startsWith("ws://") && !host.startsWith("wss://")) {
                            "ws://$host:$parsedPort/ws"
                        } else {
                            host
                        }
                        connectionManager.connect(
                            slotIndex = selectedSlot,
                            overrideConfig = ConnectionConfig(
                                linkType = socketType,
                                address = address,
                                port = parsedPort,
                                httpUrlTemplate = httpTemplate
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Connect to Slot ${selectedSlot + 1}")
            }

            OutlinedButton(
                onClick = {
                    if (mdnsDiscovery.isDiscovering) {
                        mdnsDiscovery.stopDiscovery()
                    } else {
                        mdnsDiscovery.startDiscovery()
                    }
                }
            ) {
                Text(if (mdnsDiscovery.isDiscovering) "Stop mDNS" else "Discover ESP")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (discoveredBoards.isNotEmpty()) {
            Text("Discovered Local ESP Boards (mDNS):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 200.dp)) {
                items(discoveredBoards) { esp ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            host = esp.host
                            port = esp.port.toString()
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(esp.name, fontWeight = FontWeight.Medium)
                            Text("${esp.host}:${esp.port}", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MqttTabContent(
    connectionManager: ConnectionManager,
    selectedSlot: Int
) {
    val coroutineScope = rememberCoroutineScope()
    var brokerUri by remember { mutableStateOf("tcp://broker.hivemq.com:1883") }
    var clientId by remember { mutableStateOf("EmbedHubClient_99") }
    var pubTopic by remember { mutableStateOf("esp32/cmd") }
    var subTopic by remember { mutableStateOf("esp32/telemetry") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("MQTT IoT Broker Transport", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = brokerUri,
            onValueChange = { brokerUri = it },
            label = { Text("Broker URI (e.g. tcp://ip:1883)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = clientId,
            onValueChange = { clientId = it },
            label = { Text("Client ID") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = pubTopic,
                onValueChange = { pubTopic = it },
                label = { Text("Publish Topic") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = subTopic,
                onValueChange = { subTopic = it },
                label = { Text("Subscribe Topic") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    connectionManager.connect(
                        slotIndex = selectedSlot,
                        overrideConfig = ConnectionConfig(
                            linkType = LinkType.MQTT,
                            address = brokerUri,
                            mqttClientId = clientId,
                            mqttPubTopic = pubTopic,
                            mqttSubTopics = listOf(subTopic)
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect MQTT to Slot ${selectedSlot + 1}")
        }
    }
}

@Composable
fun ProvisioningTabContent(
    connectionManager: ConnectionManager,
    selectedSlot: Int
) {
    val coroutineScope = rememberCoroutineScope()
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("ESP32 BLE WiFi Provisioner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Transfers WiFi credentials as JSON over BLE to provision a new ESP board.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = ssid,
            onValueChange = { ssid = it },
            label = { Text("WiFi Network SSID") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("WiFi Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    val jsonCmd = """{"ssid":"$ssid","pass":"$password"}"""
                    val result = connectionManager.sendCommand(selectedSlot, jsonCmd)
                    statusText = if (result.isSuccess) "Provisioning payload sent to Slot ${selectedSlot + 1}!" else "Failed: Device not connected"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Wifi, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Send Credentials to ESP32")
        }

        if (statusText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(statusText, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
    }
}
