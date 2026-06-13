package com.sportspulse.india.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sportspulse.india.features.alerts.presentation.AlertsScreen
import com.sportspulse.india.features.dashboard.presentation.DashboardScreen
import com.sportspulse.india.features.detail.presentation.DetailScreen
import com.sportspulse.india.features.venues.presentation.VenueScreen

@Composable
fun SportsPulseNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (BottomNavScreens.any { it.route == currentRoute }) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        selected = currentRoute == Screen.Dashboard.route,
                        onClick = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.LocationOn, contentDescription = "Venues") },
                        label = { Text("Venues") },
                        selected = currentRoute == Screen.Venues.route,
                        onClick = {
                            navController.navigate(Screen.Venues.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts") },
                        label = { Text("Alerts") },
                        selected = currentRoute == Screen.Alerts.route,
                        onClick = {
                            navController.navigate(Screen.Alerts.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToDetail = { eventId ->
                        navController.navigate(Screen.Detail.createRoute(eventId))
                    }
                )
            }
            
            composable(Screen.Venues.route) {
                VenueScreen()
            }
            
            composable(Screen.Alerts.route) {
                AlertsScreen()
            }
            
            composable(Screen.Detail.route) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                DetailScreen(
                    eventId = eventId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
