package com.sportspulse.india.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Venues : Screen("venues")
    object Alerts : Screen("alerts")
    
    // Detail screen takes eventId as an argument
    object Detail : Screen("detail/{eventId}") {
        fun createRoute(eventId: String) = "detail/$eventId"
    }
}

val BottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Venues,
    Screen.Alerts
)
