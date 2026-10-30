package com.embedhub.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedhub.app.core.model.LineEnding

@Composable
fun SettingsScreen() {
    var dynamicColors by remember { mutableStateOf(true) }
    var defaultRateHz by remember { mutableStateOf(20) }
    var defaultLineEnding by remember { mutableStateOf(LineEnding.CRLF) }
    var unitsMetric by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)).padding(16.dp)) {
        Text("Application Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Material You Dynamic Colors", color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Adapt colors to Android wallpaper", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(checked = dynamicColors, onCheckedChange = { dynamicColors = it })
                }

                HorizontalDivider(color = Color(0xFF334155))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Joystick Loop Rate (Hz)", color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Default continuous transmit frequency", fontSize = 11.sp, color = Color.Gray)
                    }
                    Text("${defaultRateHz} Hz", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                }

                HorizontalDivider(color = Color(0xFF334155))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Default Line Endings", color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Appended to serial and TCP commands", fontSize = 11.sp, color = Color.Gray)
                    }
                    Text(defaultLineEnding.name, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // About EmbedHub Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("About EmbedHub", fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "EmbedHub is an open-source, all-in-one controller for Arduino, ESP32, and ESP8266 boards.\n" +
                    "Replaces BLE Scanner, Serial Bluetooth Terminal, MacroDroid, Dabble, IoT Remote, Arduino Bluetooth Controller, and ESP8266 WiFi Robot Car.\n" +
                    "Version 1.0.0 (Open-Source under MIT License)",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
