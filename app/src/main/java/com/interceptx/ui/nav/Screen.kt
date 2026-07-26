package com.interceptx.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Dashboard)
    object Intercept : Screen("intercept", "Intercept", Icons.Filled.Security)
    object History : Screen("history", "History", Icons.Filled.History)
    object Repeater : Screen("repeater", "Repeater", Icons.Filled.Repeat)
    object Composer : Screen("composer", "Composer", Icons.Filled.Build)
    object Certificates : Screen("certificates", "Certificates", Icons.Filled.VerifiedUser)
    object Projects : Screen("projects", "Projects", Icons.Filled.Folder)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)

    companion object {
        val bottomBarItems = listOf(Dashboard, Intercept, History, Repeater)
        val all = listOf(Dashboard, Intercept, History, Repeater, Composer, Certificates, Projects, Settings)
    }
}
