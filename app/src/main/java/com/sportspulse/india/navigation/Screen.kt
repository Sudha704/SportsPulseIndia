package com.sportspulse.india.navigation

sealed class Screen(val route: String) {
    object BharatSportsChat : Screen("bharat_sports_chat")
    object Dashboard : Screen("dashboard")
    object Venues : Screen("venues")
    object Alerts : Screen("alerts")
    object Settings : Screen("settings")
    
    // Detail screen takes eventId as an argument
    object Detail : Screen("detail/{eventId}") {
        fun createRoute(eventId: String) = "detail/$eventId"
    }
}

val BottomNavScreens = listOf(
    Screen.BharatSportsChat,
    Screen.Venues,
    Screen.Alerts,
    Screen.Settings
)
