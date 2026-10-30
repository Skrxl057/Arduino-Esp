package com.embedhub.app.ui.screens.dashboard

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedhub.app.core.manager.ConnectionManager
import com.embedhub.app.core.model.DashboardWidget
import com.embedhub.app.core.model.WidgetType
import com.embedhub.app.core.parser.DataParser
import com.embedhub.app.ui.components.CanvasGauge
import com.embedhub.app.ui.components.CanvasLineChart
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    connectionManager: ConnectionManager,
    selectedSlot: Int
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Real-time numeric telemetry streams map (key -> history list)
    val telemetryStreams = remember { mutableStateMapOf<String, MutableList<Double>>() }

    // Configurable Widgets
    val widgets = remember {
        mutableStateListOf(
            DashboardWidget("w1", "Temperature", WidgetType.GAUGE, dataKey = "temp", min = 0.0, max = 50.0, unit = "°C", colorHex = "#0EA5E9", lastValue = 24.5),
            DashboardWidget("w2", "Telemetry Graph", WidgetType.LINE_GRAPH, dataKey = "temp", colorHex = "#10B981"),
            DashboardWidget("w3", "Humidity", WidgetType.VALUE_TILE, dataKey = "humidity", unit = "%", colorHex = "#38BDF8", lastValue = 58.0),
            DashboardWidget("w4", "Obstacle Alert", WidgetType.LED_INDICATOR, dataKey = "alert", colorHex = "#EF4444", lastValue = 0.0),
            DashboardWidget("w5", "Aux Light", WidgetType.SWITCH, dataKey = "light_sw", commandTemplate = "LIGHT:{val}", colorHex = "#F59E0B", lastValue = 1.0),
            DashboardWidget("w6", "Speed Override", WidgetType.SLIDER, dataKey = "speed", min = 0.0, max = 100.0, unit = "%", commandTemplate = "SPEED:{val}", colorHex = "#A855F7", lastValue = 75.0)
        )
    }

    // Collect incoming telemetry lines and parse automatically
    LaunchedEffect(selectedSlot) {
        connectionManager.allIncomingLines.collect { (slot, line) ->
            if (slot == selectedSlot) {
                val parsedList = DataParser.parse(line)
                for (item in parsedList) {
                    item.numericValue?.let { num ->
                        val history = telemetryStreams.getOrPut(item.key) { mutableListOf() }
                        history.add(num)
                        if (history.size > 50) history.removeAt(0)

                        // Update widgets bound to this key
                        widgets.forEachIndexed { idx, w ->
                            if (w.dataKey.equals(item.key, ignoreCase = true)) {
                                widgets[idx] = w.copy(lastValue = num, lastText = item.rawText)
                            }
                        }
                    }
                }
            }
        }
    }

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    val jsonFormat = remember { kotlinx.serialization.json.Json { ignoreUnknownKeys = true; prettyPrint = true } }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)).padding(12.dp)) {
        // Toolbar: Add widget, CSV Export, JSON Layout Export/Import
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Live Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Export Dashboard Layout to JSON
                OutlinedButton(
                    onClick = {
                        val jsonString = jsonFormat.encodeToString(widgets.toList())
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "EmbedHub-Dashboard-Layout.json")
                            putExtra(Intent.EXTRA_TEXT, jsonString)
                        }
                        context.startActivity(Intent.createChooser(intent, "Export Dashboard Layout JSON"))
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Export JSON", fontSize = 10.sp)
                }

                // Import Dashboard Layout from JSON
                OutlinedButton(
                    onClick = { showImportDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Import JSON", fontSize = 10.sp)
                }

                // Export Stream to CSV
                OutlinedButton(
                    onClick = {
                        val csvBuilder = StringBuilder("timestamp,stream,value\n")
                        val now = System.currentTimeMillis()
                        telemetryStreams.forEach { (key, vals) ->
                            vals.forEachIndexed { i, v ->
                                csvBuilder.append("${now - (vals.size - i) * 1000},$key,$v\n")
                            }
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "EmbedHub Telemetry Data.csv")
                            putExtra(Intent.EXTRA_TEXT, csvBuilder.toString())
                        }
                        context.startActivity(Intent.createChooser(intent, "Export Telemetry CSV"))
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("CSV", fontSize = 10.sp)
                }

                Button(
                    onClick = {
                        widgets.add(
                            DashboardWidget(
                                id = "w_${System.currentTimeMillis()}",
                                title = "New Stream",
                                type = WidgetType.VALUE_TILE,
                                dataKey = "sensor_${widgets.size + 1}"
                            )
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Add", fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(widgets, key = { it.id }) { widget ->
                val accentColor = remember(widget.colorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(widget.colorHex))
                    } catch (_: Exception) {
                        Color(0xFF0EA5E9)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(widget.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(widget.dataKey, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        when (widget.type) {
                            WidgetType.GAUGE -> {
                                CanvasGauge(
                                    value = widget.lastValue,
                                    min = widget.min,
                                    max = widget.max,
                                    unit = widget.unit,
                                    title = widget.title,
                                    accentColor = accentColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            WidgetType.LINE_GRAPH -> {
                                val history = telemetryStreams[widget.dataKey] ?: listOf(widget.lastValue)
                                CanvasLineChart(
                                    points = history,
                                    title = widget.title,
                                    accentColor = accentColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            WidgetType.VALUE_TILE -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 12.dp)) {
                                    Text(
                                        text = "%.2f".format(widget.lastValue),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                    if (widget.unit.isNotEmpty()) {
                                        Text(widget.unit, fontSize = 12.sp, color = Color.LightGray)
                                    }
                                }
                            }
                            WidgetType.LED_INDICATOR -> {
                                val isOn = widget.lastValue > 0.5
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isOn) accentColor else Color(0xFF334155)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = if (isOn) Color.White else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(if (isOn) "ACTIVE" else "OFF", fontSize = 11.sp, color = Color.LightGray)
                            }
                            WidgetType.SWITCH -> {
                                var isSwitched by remember { mutableStateOf(widget.lastValue > 0.5) }
                                Switch(
                                    checked = isSwitched,
                                    onCheckedChange = { checked ->
                                        isSwitched = checked
                                        coroutineScope.launch {
                                            val cmd = widget.commandTemplate.replace("{val}", if (checked) "1" else "0")
                                            connectionManager.sendCommand(selectedSlot, cmd)
                                        }
                                    }
                                )
                            }
                            WidgetType.SLIDER -> {
                                var sVal by remember { mutableStateOf(widget.lastValue.toFloat()) }
                                Slider(
                                    value = sVal,
                                    onValueChange = { sVal = it },
                                    onValueChangeFinished = {
                                        coroutineScope.launch {
                                            val cmd = widget.commandTemplate.replace("{val}", sVal.toInt().toString())
                                            connectionManager.sendCommand(selectedSlot, cmd)
                                        }
                                    },
                                    valueRange = widget.min.toFloat()..widget.max.toFloat()
                                )
                                Text("%.0f %s".format(sVal, widget.unit), color = Color.LightGray, fontSize = 11.sp)
                            }
                            else -> {
                                Text(widget.lastText.ifEmpty { "%.1f".format(widget.lastValue) }, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Dashboard Layout JSON") },
            text = {
                Column {
                    Text("Paste previously exported Dashboard JSON below:", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        placeholder = { Text("Paste JSON here...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val imported = jsonFormat.decodeFromString<List<DashboardWidget>>(importJsonText)
                            widgets.clear()
                            widgets.addAll(imported)
                            showImportDialog = false
                            importJsonText = ""
                        } catch (_: Exception) {
                            // Try parsing wrapped object
                            showImportDialog = false
                        }
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
