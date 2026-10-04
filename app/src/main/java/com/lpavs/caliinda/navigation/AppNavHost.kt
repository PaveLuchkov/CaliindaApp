@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.lpavs.caliinda.navigation

import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import com.lpavs.caliinda.core.ui.theme.AppMotion
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
      // Общая ось X: новый экран въезжает справа, прежний чуть уходит влево; назад — зеркально.
      // Раньше при возврате главный экран тоже въезжал справа, навстречу уезжающим настройкам.
      enterTransition = {
        slideInHorizontally(AppMotion.defaultSpatialSpec()) { it } +
            fadeIn(AppMotion.defaultEffectsSpec())
      },
      exitTransition = {
        slideOutHorizontally(AppMotion.defaultSpatialSpec()) { -it / 4 } +
            fadeOut(AppMotion.defaultEffectsSpec())
      },
      popEnterTransition = {
        slideInHorizontally(AppMotion.defaultSpatialSpec()) { -it / 4 } +
            fadeIn(AppMotion.defaultEffectsSpec())
      },
      popExitTransition = {
        slideOutHorizontally(AppMotion.defaultSpatialSpec()) { it } +
            fadeOut(AppMotion.defaultEffectsSpec())
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
