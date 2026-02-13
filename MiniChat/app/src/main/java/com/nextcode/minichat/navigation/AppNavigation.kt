package com.nextcode.minichat.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nextcode.minichat.ui.screens.*
import com.nextcode.minichat.ui.viewmodels.AuthViewModel
import com.nextcode.minichat.ui.viewmodels.AuthEvent

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
        composable(Screen.Auth.route) {
            val viewModel: AuthViewModel = hiltViewModel<AuthViewModel>(it)
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Permissions.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onNavigateToVerify = {
                    navController.navigate(Screen.VerifyEmail.route)
                },
                viewModel = viewModel
            )
        }
        composable(Screen.VerifyEmail.route) {
            val parentEntry = remember(it) { navController.getBackStackEntry(Screen.Auth.route) }
            val viewModel: AuthViewModel = hiltViewModel<AuthViewModel>(parentEntry)
            VerifyEmailScreen(
                onVerificationSuccess = {
                    navController.navigate(Screen.Permissions.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onBackToAuth = {
                    navController.popBackStack()
                },
                viewModel = viewModel
            )
        }
        composable(Screen.Permissions.route) {
            PermissionScreen(navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.Settings.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            SettingsScreen(
                navController = navController,
                onLogout = {
                    viewModel.onEvent(AuthEvent.Logout)
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Report.route) {
            ReportScreen(navController)
        }
        composable(Screen.Premium.route) {
            PremiumScreen(navController)
        }
    }
}
