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
            // 2. Передаем полученный viewModel в MainScreen
            MainScreen(
                viewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.Settings.route)
                }
            )
        }
        composable(NavRoutes.Settings.route) {
            // 3. Передаем полученный viewModel в SettingsScreen
            SettingsScreen(
                viewModel = viewModel,
                onSignInClick = onSignInClick, // Передаем лямбду для кнопки
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