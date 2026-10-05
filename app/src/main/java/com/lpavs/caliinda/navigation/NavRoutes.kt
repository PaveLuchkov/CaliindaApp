package com.lpavs.caliinda.navigation

sealed class NavRoutes(val route: String) {
  object Main : NavRoutes("main")

  object Settings : NavRoutes("settings")
}
