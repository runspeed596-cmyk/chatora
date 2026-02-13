package com.nextcode.minichat.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth")
    object Permissions : Screen("permissions")
    object Home : Screen("home")
    object Matching : Screen("matching")
    object VideoCall : Screen("video_call")
    object Report : Screen("report")
    object Settings : Screen("settings")
    object VerifyEmail : Screen("verify_email")
    object Premium : Screen("premium")
}
