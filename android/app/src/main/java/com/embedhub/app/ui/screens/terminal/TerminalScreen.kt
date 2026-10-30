package com.embedhub.app.ui.screens.terminal

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedhub.app.core.manager.ConnectionManager
import com.embedhub.app.core.model.LineEnding
import com.embedhub.app.core.model.LogMessage
import com.embedhub.app.core.model.MacroButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class DisplayMode {
    TEXT,
    HEX,
    BOTH
}

@Composable
fun TerminalScreen(
    connectionManager: ConnectionManager,
    selectedSlot: Int
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputCommand by remember { mutableStateOf("") }
    var selectedEnding by remember { mutableStateOf(LineEnding.CRLF) }
    var displayMode by remember { mutableStateOf(DisplayMode.TEXT) }
    var autoScroll by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    val logs = remember { mutableStateListOf<LogMessage>() }
    val macros = remember {
        mutableStateListOf(
            MacroButton("m1", "Ping", "PING", LineEnding.CRLF),
            MacroButton("m2", "Status", "STATUS?", LineEnding.CRLF),
            MacroButton("m3", "LED ON", "LED:1", LineEnding.CRLF),
            MacroButton("m4", "LED OFF", "LED:0", LineEnding.CRLF)
        )
    }

    // Collect incoming lines from connection manager
    LaunchedEffect(selectedSlot) {
        connectionManager.allIncomingBytes.collect { (slot, bytes) ->
            if (slot == selectedSlot) {
                val log = LogMessage(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    isTx = false,
                    text = String(bytes, Charsets.UTF_8).trimEnd(),
                    rawBytes = bytes,
                    slotIndex = slot
                )
                logs.add(log)
                if (autoScroll && logs.isNotEmpty()) {
                    listState.animateScrollToItem(logs.size - 1)
                }
            }
        }
    }

    val filteredLogs = remember(logs, searchQuery) {
        if (searchQuery.isEmpty()) logs
        else logs.filter { it.text.contains(searchQuery, ignoreCase = true) || it.hexString.contains(searchQuery, ignoreCase = true) }
    }

    val slots by connectionManager.slots.collectAsState()
    val currentSlot = slots[selectedSlot]

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        // Stats & Toolbar Header
        Surface(color = Color(0xFF1E293B), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("TX: ${currentSlot.txBytes} B", color = Color(0xFF38BDF8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("RX: ${currentSlot.rxBytes} B", color = Color(0xFF4ADE80), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Display mode toggle
                    TextButton(
                        onClick = {
                            displayMode = when (displayMode) {
                                DisplayMode.TEXT -> DisplayMode.HEX
                                DisplayMode.HEX -> DisplayMode.BOTH
                                DisplayMode.BOTH -> DisplayMode.TEXT
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text(displayMode.name, fontSize = 11.sp, color = Color.White)
                    }

                    // Auto scroll toggle
                    IconButton(onClick = { autoScroll = !autoScroll }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.VerticalAlignBottom,
                            contentDescription = "Auto Scroll",
                            tint = if (autoScroll) Color(0xFF38BDF8) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Export / Share log
                    IconButton(
                        onClick = {
                            val textDump = logs.joinToString("\n") {
                                val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(it.timestamp))
                                val dir = if (it.isTx) "TX" else "RX"
                                "[$time] [$dir] ${it.text}"
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "EmbedHub Terminal Log")
                                putExtra(Intent.EXTRA_TEXT, textDump)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Terminal Log"))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Log", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }

                    // Clear logs
                    IconButton(onClick = { logs.clear() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Search Filter Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter log output...", fontSize = 12.sp, color = Color.Gray) },
            modifier = Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 8.dp, vertical = 2.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White),
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        )

        // Terminal Log List
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(filteredLogs, key = { it.id }) { log ->
                val timeStr = remember(log.timestamp) {
                    SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(log.timestamp))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "[$timeStr] ",
                        color = Color(0xFF64748B),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        text = if (log.isTx) "TX ▶ " else "RX ◀ ",
                        color = if (log.isTx) Color(0xFF38BDF8) else Color(0xFF4ADE80),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Column {
                        if (displayMode == DisplayMode.TEXT || displayMode == DisplayMode.BOTH) {
                            Text(
                                text = log.text,
                                color = if (log.isTx) Color(0xFFE2E8F0) else Color(0xFFF8FAFC),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                        if (displayMode == DisplayMode.HEX || displayMode == DisplayMode.BOTH) {
                            Text(
                                text = log.hexString,
                                color = Color(0xFFFCD34D),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Saved Macros Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            macros.forEach { macro ->
                FilledTonalButton(
                    onClick = {
                        coroutineScope.launch {
                            val bytes = (macro.command + macro.lineEnding.chars).toByteArray(Charsets.UTF_8)
                            connectionManager.sendCommand(selectedSlot, macro.command, macro.lineEnding)
                            logs.add(
                                LogMessage(
                                    id = UUID.randomUUID().toString(),
                                    timestamp = System.currentTimeMillis(),
                                    isTx = true,
                                    text = macro.command,
                                    rawBytes = bytes,
                                    slotIndex = selectedSlot
                                )
                            )
                            if (autoScroll && logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(macro.label, fontSize = 11.sp)
                }
            }
        }

        // Send Input Box & Line Endings
        Surface(color = Color(0xFF1E293B), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Line Ending Dropdown Selector
                var showEndingMenu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { showEndingMenu = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(selectedEnding.name, fontSize = 11.sp)
                    }
                    DropdownMenu(
                        expanded = showEndingMenu,
                        onDismissRequest = { showEndingMenu = false }
                    ) {
                        LineEnding.values().forEach { ending ->
                            DropdownMenuItem(
                                text = { Text(ending.name) },
                                onClick = {
                                    selectedEnding = ending
                                    showEndingMenu = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = inputCommand,
                    onValueChange = { inputCommand = it },
                    placeholder = { Text("Enter command...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (inputCommand.isNotEmpty()) {
                            val cmd = inputCommand
                            inputCommand = ""
                            coroutineScope.launch {
                                val bytes = (cmd + selectedEnding.chars).toByteArray(Charsets.UTF_8)
                                connectionManager.sendCommand(selectedSlot, cmd, selectedEnding)
                                logs.add(
                                    LogMessage(
                                        id = UUID.randomUUID().toString(),
                                        timestamp = System.currentTimeMillis(),
                                        isTx = true,
                                        text = cmd,
                                        rawBytes = bytes,
                                        slotIndex = selectedSlot
                                    )
                                )
                                if (autoScroll && logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
                            }
                        }
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}
