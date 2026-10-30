package com.embedhub.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Connections : Screen("connections", "Connections", Icons.Default.Bluetooth)
    object Terminal : Screen("terminal", "Terminal", Icons.Default.Terminal)
    object Control : Screen("control", "Control", Icons.Default.Gamepad)
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Automation : Screen("automation", "Automation", Icons.Default.AutoMode)
    object Profiles : Screen("profiles", "Profiles", Icons.Default.FolderShared)
    object Code : Screen("code", "Code & OTA", Icons.Default.Code)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val BottomNavItems = listOf(
    Screen.Connections,
    Screen.Terminal,
    Screen.Control,
    Screen.Dashboard,
    Screen.Automation,
    Screen.Code
)
