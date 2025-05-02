package com.example.caliindar.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.caliindar.ui.screens.settings.SettingsScreen
import com.example.caliindar.ui.screens.main.MainScreen
import com.example.caliindar.ui.screens.main.MainViewModel
import com.example.caliindar.ui.screens.settings.AISettingsScreen
import com.example.caliindar.ui.screens.settings.TermsOfUseScreen
import com.example.caliindar.ui.screens.settings.TimeSettingsScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel,
    onSignInClick: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Main.route, // Используем объект роута
        modifier = modifier // Применяем Modifier
    ) {
        composable(NavRoutes.Main.route) {
            MainScreen(
                viewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.Settings.route)
                }
            )
        }
        composable(NavRoutes.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onSignInClick = onSignInClick, // Передаем лямбду для кнопки
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAISettings = {
                    navController.navigate(NavRoutes.AISettings.route)
                },
                onNavigateToTimeSettings = {
                    navController.navigate(NavRoutes.TimeSettings.route)
                },
                onNavigateToTermsOfuse = {
                    navController.navigate(NavRoutes.Terms.route)
                }
            )
        }
        composable(NavRoutes.AISettings.route) {
            AISettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.TimeSettings.route) {
            TimeSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                title = "Time & Format"
            )
        }
        composable(NavRoutes.Terms.route) {
            TermsOfUseScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                title = "Terms of Use"
            )
        }
    }
}

// Роуты остаются без изменений
sealed class NavRoutes(val route: String) {
    object Main : NavRoutes("main")
    object Settings : NavRoutes("settings")
    object AISettings : NavRoutes("aisettings")
    object TimeSettings : NavRoutes("timesettings")
    object Terms : NavRoutes("termsofuse")
}