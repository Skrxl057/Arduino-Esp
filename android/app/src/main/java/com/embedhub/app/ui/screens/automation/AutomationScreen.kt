package com.embedhub.app.ui.screens.automation

import androidx.compose.foundation.background
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
import com.embedhub.app.core.model.*
import com.embedhub.app.core.rules.RuleExecutionLog
import com.embedhub.app.core.rules.RulesEngine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AutomationScreen(
    connectionManager: ConnectionManager,
    rulesEngine: RulesEngine
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0: Rules, 1: History Log
    val tabs = listOf("Active Rules", "Execution Log")

    val executionLogs = remember { mutableStateListOf<RuleExecutionLog>() }

    LaunchedEffect(Unit) {
        rulesEngine.executionHistory.collect { log ->
            executionLogs.add(0, log)
            if (executionLogs.size > 100) executionLogs.removeAt(executionLogs.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, fontSize = 13.sp) }
                )
            }
        }

        if (selectedTab == 0) {
            RulesListTab(rulesEngine)
        } else {
            HistoryLogTab(executionLogs)
        }
    }
}

@Composable
fun RulesListTab(rulesEngine: RulesEngine) {
    val rules = remember {
        mutableStateListOf(
            AutomationRule(
                id = "rule_temp_fan",
                name = "High Temp Fan Controller",
                isEnabled = true,
                cooldownMs = 3000L,
                trigger = RuleTrigger.DataReceived(key = "temp", operator = ">", threshold = 35.0),
                actions = listOf(
                    RuleAction.SendCommand(targetSlot = 0, command = "RELAY:FAN:ON"),
                    RuleAction.ShowNotification("High Heat Alert", "Temp exceeded 35°C. Cooling fan activated.")
                )
            ),
            AutomationRule(
                id = "rule_obstacle_stop",
                name = "Obstacle Failsafe Stop",
                isEnabled = true,
                cooldownMs = 1000L,
                trigger = RuleTrigger.DataReceived(key = "dist", operator = "<", threshold = 20.0),
                actions = listOf(
                    RuleAction.SendCommand(targetSlot = 0, command = "S"),
                    RuleAction.HapticVibrate(400)
                )
            ),
            AutomationRule(
                id = "rule_disconnect_failsafe",
                name = "Link Disconnect Alarm",
                isEnabled = true,
                cooldownMs = 5000L,
                trigger = RuleTrigger.ConnectionEvent(targetSlot = -1, isConnected = false),
                actions = listOf(
                    RuleAction.ShowNotification("Link Lost", "Robot car disconnected. Motors engaged to failsafe STOP."),
                    RuleAction.HapticVibrate(800)
                )
            )
        )
    }

    LaunchedEffect(rules) {
        rulesEngine.setRules(rules)
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MacroDroid Rules Engine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Button(
                onClick = {
                    rules.add(
                        AutomationRule(
                            id = "rule_${System.currentTimeMillis()}",
                            name = "New Automation Rule",
                            trigger = RuleTrigger.DataReceived("voltage", "<", 3.3),
                            actions = listOf(RuleAction.ShowNotification("Battery Low", "Charge hardware"))
                        )
                    )
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Rule", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rules, key = { it.id }) { rule ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rule.name, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Cooldown: ${rule.cooldownMs / 1000}s", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = rule.isEnabled,
                                onCheckedChange = { checked ->
                                    val idx = rules.indexOfFirst { it.id == rule.id }
                                    if (idx != -1) rules[idx] = rule.copy(isEnabled = checked)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Trigger info
                        Text(
                            text = "TRIGGER: ${
                                when (val t = rule.trigger) {
                                    is RuleTrigger.DataReceived -> "When '${t.key}' ${t.operator} ${t.threshold}"
                                    is RuleTrigger.DataChanged -> "When '${t.key}' changes"
                                    is RuleTrigger.ConnectionEvent -> "Connection ${if (t.isConnected) "Established" else "Lost"}"
                                    is RuleTrigger.PhoneEvent -> "Phone event: ${t.eventType}"
                                    else -> "Scheduled interval"
                                }
                            }",
                            fontSize = 12.sp,
                            color = Color(0xFF38BDF8)
                        )

                        // Actions info
                        Text(
                            text = "ACTIONS: ${rule.actions.size} actions configured",
                            fontSize = 12.sp,
                            color = Color(0xFF4ADE80)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { rulesEngine.testRun(rule) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Run", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { rules.removeAll { it.id == rule.id } },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryLogTab(logs: List<RuleExecutionLog>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (logs.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No rules executed yet", color = Color.Gray)
                }
            }
        }
        items(logs) { log ->
            val time = remember(log.timestamp) {
                SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(log.timestamp))
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(log.ruleName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text(log.actionSummary, fontSize = 11.sp, color = Color.LightGray)
                    }
                    Text(time, fontSize = 11.sp, color = Color.Gray, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
        }
    }
}
