@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.lpavs.caliinda.navigation

import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import com.lpavs.caliinda.core.ui.theme.AppMotion
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.app.LaunchRequests
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lpavs.caliinda.feature.calendar.presentation.CalendarScreen
import com.lpavs.caliinda.feature.calendar.presentation.CalendarViewModel
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import com.lpavs.caliinda.feature.settings.ui.ChipsSettingsScreen
import com.lpavs.caliinda.feature.settings.ui.SettingsScreen
import com.lpavs.caliinda.feature.settings.vm.ChipsSettingsViewModel
import com.lpavs.caliinda.feature.settings.vm.SettingsViewModel

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
  // Просьба снаружи (виджет, другое приложение) — форма и подробности живут на главном экране,
  // уходим на него, если открыты настройки.
  val createRequested by LaunchRequests.createEvent.collectAsStateWithLifecycle()
  val openRequest by LaunchRequests.openEvent.collectAsStateWithLifecycle()
  val insertRequest by LaunchRequests.insertEvent.collectAsStateWithLifecycle()
  val openTime by LaunchRequests.openTime.collectAsStateWithLifecycle()
  val requested =
      createRequested || openRequest != null || insertRequest != null || openTime != null
  LaunchedEffect(requested) {
    if (requested) navController.popBackStack(NavRoutes.Main.route, inclusive = false)
  }
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

      CalendarScreen(
          calendarViewModel = calendarViewModel,
          onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) },
          eventManagementViewModel = eventManagementViewModel)
    }
    composable(
        NavRoutes.Settings.route,
    ) {
        val settingsViewModel: SettingsViewModel = hiltViewModel()

      SettingsScreen(
          settingsViewModel = settingsViewModel,
          onNavigateBack = { navController.popBackStack() },
          onNavigateToChips = { navController.navigate(NavRoutes.SuggestionChips.route) })
    }
    composable(NavRoutes.SuggestionChips.route) {
      ChipsSettingsScreen(
          viewModel = hiltViewModel<ChipsSettingsViewModel>(),
          onNavigateBack = { navController.popBackStack() })
    }
  }
}
