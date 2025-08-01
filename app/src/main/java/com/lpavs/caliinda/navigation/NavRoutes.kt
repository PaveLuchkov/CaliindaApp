package com.lpavs.caliinda.navigation

sealed class NavRoutes(val route: String) {
  object Main : NavRoutes("main")
  object Settings : NavRoutes("settings")
  object AISettings : NavRoutes("aiSettings")
  object TimeSettings : NavRoutes("timeSettings")
  object Terms : NavRoutes("termsOfUse")
}