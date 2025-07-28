package com.lpavs.caliinda.navigation

import android.app.Activity
import android.util.Log
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lpavs.caliinda.ui.screens.main.MainScreen
import com.lpavs.caliinda.ui.screens.main.MainViewModel
import com.lpavs.caliinda.ui.screens.settings.AISettingsScreen
import com.lpavs.caliinda.ui.screens.settings.SettingsScreen
import com.lpavs.caliinda.ui.screens.settings.TermsOfUseScreen
import com.lpavs.caliinda.ui.screens.settings.TimeSettingsScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val onSignInClick: () -> Unit = {
        if (activity != null) {
            viewModel.signIn(activity)
        } else {
            Log.e("AppNavHost", "Activity is null, cannot perform sign-in.")
        }
    }
  NavHost(
      navController = navController,
      startDestination = NavRoutes.Main.route,
      popEnterTransition = {
          slideInHorizontally(
              initialOffsetX = { fullWidth -> fullWidth },
              animationSpec = tween(durationMillis = 150, easing = EaseOut)
          )
      },
      popExitTransition = {
          slideOutHorizontally(
              targetOffsetX = { fullWidth -> fullWidth },
              animationSpec = tween(durationMillis = 150, easing = EaseIn)
          )
      },
      enterTransition = {
          slideInHorizontally(
              initialOffsetX = { it },
              animationSpec = tween(200)
          )
      },
      modifier = modifier,
  ) {
        composable(
            NavRoutes.Main.route,
        ) {
              MainScreen(
                  viewModel = viewModel,
                  onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) })
            }
        composable(
            NavRoutes.Settings.route,
        ) {
              SettingsScreen(
                  viewModel = viewModel,
                  onSignInClick = onSignInClick,
                  onNavigateBack = { navController.popBackStack() },
                  onNavigateToAISettings = { navController.navigate(NavRoutes.AISettings.route) },
                  onNavigateToTimeSettings = {
                    navController.navigate(NavRoutes.TimeSettings.route)
                  },
                  onNavigateToTermsOfuse = { navController.navigate(NavRoutes.Terms.route) })
            }
        composable(
            NavRoutes.AISettings.route,
            ) {
              AISettingsScreen(
                  viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
            }
        composable(
            NavRoutes.TimeSettings.route,
            ) {
              TimeSettingsScreen(
                  viewModel = viewModel,
                  onNavigateBack = { navController.popBackStack() },
                  title = "Time & Format")
            }
        composable(
            NavRoutes.Terms.route,
            ) {
              TermsOfUseScreen(
                  onNavigateBack = { navController.popBackStack() }, title = "Terms of Use")
            }
      }
}

sealed class NavRoutes(val route: String) {
  object Main : NavRoutes("main")

  object Settings : NavRoutes("settings")

  object AISettings : NavRoutes("aisettings")

  object TimeSettings : NavRoutes("timesettings")

  object Terms : NavRoutes("termsofuse")
}
