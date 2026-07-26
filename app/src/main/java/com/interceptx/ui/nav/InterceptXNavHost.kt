package com.interceptx.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.interceptx.InterceptXApp
import com.interceptx.ui.screens.certificates.CertificatesScreen
import com.interceptx.ui.screens.composer.ComposerScreen
import com.interceptx.ui.screens.dashboard.DashboardScreen
import com.interceptx.ui.screens.history.HistoryScreen
import com.interceptx.ui.screens.intercept.InterceptScreen
import com.interceptx.ui.screens.projects.ProjectsScreen
import com.interceptx.ui.screens.repeater.RepeaterScreen
import com.interceptx.ui.screens.settings.SettingsScreen
import com.interceptx.viewmodel.ViewModelFactory

@Composable
fun InterceptXNavHost(app: InterceptXApp) {
    val navController = rememberNavController()
    val factory = remember { ViewModelFactory(app) }

    Scaffold(
        bottomBar = { InterceptXBottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel(factory = factory),
                    onNavigate = { navController.navigate(it) }
                )
            }
            composable(Screen.Intercept.route) {
                InterceptScreen(viewModel = viewModel(factory = factory))
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    viewModel = viewModel(factory = factory),
                    repeaterViewModel = viewModel(factory = factory)
                )
            }
            composable(Screen.Repeater.route) {
                RepeaterScreen(viewModel = viewModel(factory = factory))
            }
            composable(Screen.Composer.route) {
                ComposerScreen()
            }
            composable(Screen.Certificates.route) {
                CertificatesScreen(viewModel = viewModel(factory = factory))
            }
            composable(Screen.Projects.route) {
                ProjectsScreen(viewModel = viewModel(factory = factory))
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel(factory = factory))
            }
        }
    }
}

@Composable
private fun InterceptXBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        Screen.bottomBarItems.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) }
            )
        }
    }
}
