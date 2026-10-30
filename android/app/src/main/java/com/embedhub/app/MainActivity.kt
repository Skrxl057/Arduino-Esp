package com.embedhub.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.content.pm.ActivityInfo
import com.embedhub.app.core.model.ScreenOrientationMode
import com.embedhub.app.core.rules.RulesEngine
import com.embedhub.app.core.transport.MdnsDiscovery
import com.embedhub.app.ui.components.MultiDeviceBar
import com.embedhub.app.ui.components.PermissionCheckContainer
import com.embedhub.app.ui.navigation.BottomNavItems
import com.embedhub.app.ui.navigation.Screen
import com.embedhub.app.ui.screens.automation.AutomationScreen
import com.embedhub.app.ui.screens.code.CodeScreen
import com.embedhub.app.ui.screens.connections.ConnectionsScreen
import com.embedhub.app.ui.screens.control.ControlScreen
import com.embedhub.app.ui.screens.dashboard.DashboardScreen
import com.embedhub.app.ui.screens.profiles.ProfilesScreen
import com.embedhub.app.ui.screens.settings.SettingsScreen
import com.embedhub.app.ui.screens.terminal.TerminalScreen
import com.embedhub.app.ui.theme.EmbedHubTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as EmbedHubApp
        val connectionManager = app.connectionManager
        val profileRepository = app.profileRepository
        val mdnsDiscovery = MdnsDiscovery(this)

        setContent {
            EmbedHubTheme {
                PermissionCheckContainer {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    var selectedSlot by remember { mutableStateOf(0) }
                    val slots by connectionManager.slots.collectAsState()
                    val coroutineScope = rememberCoroutineScope()
                    var orientationMode by remember { mutableStateOf(ScreenOrientationMode.AUTO_SENSOR) }

                    fun cycleOrientation() {
                        val nextMode = when (orientationMode) {
                            ScreenOrientationMode.AUTO_SENSOR -> ScreenOrientationMode.LOCKED_LANDSCAPE
                            ScreenOrientationMode.LOCKED_LANDSCAPE -> ScreenOrientationMode.LOCKED_PORTRAIT
                            ScreenOrientationMode.LOCKED_PORTRAIT -> ScreenOrientationMode.AUTO_SENSOR
                            ScreenOrientationMode.REVERSE_LANDSCAPE -> ScreenOrientationMode.AUTO_SENSOR
                        }
                        orientationMode = nextMode
                        requestedOrientation = when (nextMode) {
                            ScreenOrientationMode.AUTO_SENSOR -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
                            ScreenOrientationMode.LOCKED_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            ScreenOrientationMode.LOCKED_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            ScreenOrientationMode.REVERSE_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        }
                    }

                    val rulesEngine = remember {
                        RulesEngine(
                            scope = coroutineScope,
                            onSendCommand = { slot, cmd ->
                                connectionManager.sendCommand(slot, cmd)
                            },
                            onShowNotification = { _, _ -> },
                            onVibrate = { _ -> },
                            onSpeak = { _ -> }
                        )
                    }

                    Scaffold(
                        topBar = {
                            MultiDeviceBar(
                                slots = slots,
                                selectedSlotIndex = selectedSlot,
                                onSelectSlot = { selectedSlot = it },
                                orientationMode = orientationMode,
                                onToggleOrientation = { cycleOrientation() }
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = Color(0xFF1E293B),
                                tonalElevation = 4.dp
                            ) {
                                BottomNavItems.forEach { screen ->
                                    val isSelected = currentRoute == screen.route
                                    NavigationBarItem(
                                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                                        label = { Text(screen.title, fontSize = 10.sp) },
                                        selected = isSelected,
                                        onClick = {
                                            if (currentRoute != screen.route) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color(0xFF38BDF8),
                                            selectedTextColor = Color(0xFF38BDF8),
                                            unselectedIconColor = Color(0xFF94A3B8),
                                            unselectedTextColor = Color(0xFF94A3B8),
                                            indicatorColor = Color(0xFF0F172A)
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Connections.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Connections.route) {
                                ConnectionsScreen(
                                    connectionManager = connectionManager,
                                    selectedSlot = selectedSlot,
                                    mdnsDiscovery = mdnsDiscovery
                                )
                            }
                            composable(Screen.Terminal.route) {
                                TerminalScreen(
                                    connectionManager = connectionManager,
                                    selectedSlot = selectedSlot
                                )
                            }
                            composable(Screen.Control.route) {
                                ControlScreen(
                                    connectionManager = connectionManager,
                                    selectedSlot = selectedSlot,
                                    orientationMode = orientationMode,
                                    onToggleOrientation = { cycleOrientation() }
                                )
                            }
                            composable(Screen.Dashboard.route) {
                                DashboardScreen(
                                    connectionManager = connectionManager,
                                    selectedSlot = selectedSlot
                                )
                            }
                            composable(Screen.Automation.route) {
                                AutomationScreen(
                                    connectionManager = connectionManager,
                                    rulesEngine = rulesEngine
                                )
                            }
                            composable(Screen.Profiles.route) {
                                ProfilesScreen(
                                    connectionManager = connectionManager,
                                    profileRepository = profileRepository,
                                    selectedSlot = selectedSlot
                                )
                            }
                            composable(Screen.Code.route) {
                                CodeScreen()
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}
