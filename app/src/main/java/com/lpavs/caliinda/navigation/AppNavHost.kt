package com.lpavs.caliinda.navigation

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lpavs.caliinda.feature.calendar.presentation.CalendarScreen
import com.lpavs.caliinda.feature.calendar.presentation.CalendarViewModel
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import com.lpavs.caliinda.feature.settings.ui.CalendarSettingsScreen
import com.lpavs.caliinda.feature.settings.ui.SettingsScreen
import com.lpavs.caliinda.feature.settings.ui.AboutScreen
import com.lpavs.caliinda.feature.settings.ui.TimeSettingsScreen
import com.lpavs.caliinda.feature.settings.vm.SettingsViewModel

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
  NavHost(
      navController = navController,
      startDestination = NavRoutes.Main.route,
      popEnterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 150, easing = EaseOut))
      },
      popExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 150, easing = EaseIn))
      },
      enterTransition = {
        slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(200))
      },
      modifier = modifier,
  ) {
    composable(
        NavRoutes.Main.route,
    ) {
      val eventManagementViewModel: EventManagementViewModel = hiltViewModel()
      val calendarViewModel: CalendarViewModel = hiltViewModel()
        val settingsViewModel: SettingsViewModel = hiltViewModel()

      CalendarScreen(
          calendarViewModel = calendarViewModel,
          onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) },
          eventManagementViewModel = eventManagementViewModel,
          settignsViewModel = settingsViewModel)
    }
    composable(
        NavRoutes.Settings.route,
    ) {
        val settingsViewModel: SettingsViewModel = hiltViewModel()

      SettingsScreen(
          settignsViewModel = settingsViewModel,
          onNavigateBack = { navController.popBackStack() },
          onNavigateToCalendarSettings = {
            navController.navigate(NavRoutes.CalendarSettings.route)
          },
          onNavigateToTimeSettings = { navController.navigate(NavRoutes.TimeSettings.route) },
          onNavigateToAbout = { navController.navigate(NavRoutes.Terms.route) })
    }
    composable(
        NavRoutes.CalendarSettings.route,
    ) {
        val settingsViewModel: SettingsViewModel = hiltViewModel()
      CalendarSettingsScreen(
          viewModel = settingsViewModel, onNavigateBack = { navController.popBackStack() })
    }
    composable(
        NavRoutes.TimeSettings.route,
    ) {
      val settingsViewModel: SettingsViewModel = hiltViewModel()
      TimeSettingsScreen(
          viewModel = settingsViewModel,
          onNavigateBack = { navController.popBackStack() },
          title = "Time & Format")
    }
    composable(
        NavRoutes.Terms.route,
    ) {
      AboutScreen(onNavigateBack = { navController.popBackStack() }, title = "About")
    }
  }
}
