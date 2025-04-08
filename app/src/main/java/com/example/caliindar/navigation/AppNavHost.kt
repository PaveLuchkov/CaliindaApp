package com.example.caliindar.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.caliindar.ui.screens.settings.SettingsScreen
import com.example.caliindar.ui.screens.main.MainScreen
import com.example.caliindar.ui.screens.main.MainViewModel

@Composable
fun AppNavHost(
    onSignInClick: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = NavRoutes.Main.route) {
        composable(NavRoutes.Main.route) {
            MainScreen(
                // viewModel = hiltViewModel(), // Можно так, или внутри самого MainScreen
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.Settings.route)
                }
            )
        }
        composable(NavRoutes.Settings.route) {
            SettingsScreen(
                onSignInClick = onSignInClick,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

// Роуты остаются без изменений
sealed class NavRoutes(val route: String) {
    object Main : NavRoutes("main")
    object Settings : NavRoutes("settings")
}