package com.example.caliindar.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.caliindar.ui.screens.settings.SettingsScreen
import com.example.caliindar.ui.screens.main.MainScreen
import com.example.caliindar.ui.screens.main.MainViewModel
import com.example.caliindar.ui.screens.main.components.CreateEventScreen
import com.example.caliindar.ui.screens.settings.AISettingsScreen
import com.example.caliindar.ui.screens.settings.TermsOfUseScreen
import com.example.caliindar.ui.screens.settings.TimeSettingsScreen
import java.time.LocalDate

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel,
    onSignInClick: () -> Unit
) {
    val slideDuration = 300
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Main.route, // Используем объект роута
        modifier = modifier, // Применяем Modifier
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None }
    ) {
        composable(
            NavRoutes.Main.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start, // Въезжает слева
                    animationSpec = tween(slideDuration, easing = EaseOut)
                ) // Можно добавить + fadeIn() для плавности
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start, // Уезжает влево (чуть медленнее/меньше, чтобы создать эффект глубины)
                    animationSpec = tween(slideDuration, easing = EaseIn),
                    targetOffset = { fullWidth -> -fullWidth / 4 } // Смещаем не полностью за экран сразу
                ) // Можно добавить + fadeOut()
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End, // Возвращается справа
                    animationSpec = tween(slideDuration, easing = EaseOut),
                    initialOffset = { fullWidth -> -fullWidth / 4 } // Начинает с той же позиции, куда уехал exit
                ) // Можно добавить + fadeIn()
            },
            popExitTransition = { // <-- САМАЯ ВАЖНАЯ для предиктивного жеста
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End, // Уезжает вправо при жесте "назад"
                    animationSpec = tween(slideDuration, easing = EaseIn)
                ) // Можно добавить + fadeOut()
            }
        ) {
            MainScreen(
                viewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.Settings.route)
                },
                navController = navController
            )

        }
        composable(NavRoutes.Settings.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start, // Въезжает слева
                    animationSpec = tween(slideDuration, easing = EaseOut)
                ) // Можно добавить + fadeIn() для плавности
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start, // Уезжает влево (чуть медленнее/меньше, чтобы создать эффект глубины)
                    animationSpec = tween(slideDuration, easing = EaseIn),
                    targetOffset = { fullWidth -> -fullWidth / 4 } // Смещаем не полностью за экран сразу
                ) // Можно добавить + fadeOut()
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End, // Возвращается справа
                    animationSpec = tween(slideDuration, easing = EaseOut),
                    initialOffset = { fullWidth -> -fullWidth / 4 } // Начинает с той же позиции, куда уехал exit
                ) // Можно добавить + fadeIn()
            },
            popExitTransition = { // <-- САМАЯ ВАЖНАЯ для предиктивного жеста
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End, // Уезжает вправо при жесте "назад"
                    animationSpec = tween(slideDuration, easing = EaseIn)
                ) // Можно добавить + fadeOut()
            }
        ) {
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
        composable(
            NavRoutes.AISettings.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start, // Въезжает слева
                    animationSpec = tween(slideDuration, easing = EaseOut)
                ) // Можно добавить + fadeIn() для плавности
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start, // Уезжает влево (чуть медленнее/меньше, чтобы создать эффект глубины)
                    animationSpec = tween(slideDuration, easing = EaseIn),
                    targetOffset = { fullWidth -> -fullWidth / 4 } // Смещаем не полностью за экран сразу
                ) // Можно добавить + fadeOut()
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End, // Возвращается справа
                    animationSpec = tween(slideDuration, easing = EaseOut),
                    initialOffset = { fullWidth -> -fullWidth / 4 } // Начинает с той же позиции, куда уехал exit
                ) // Можно добавить + fadeIn()
            },
            popExitTransition = { // <-- САМАЯ ВАЖНАЯ для предиктивного жеста
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End, // Уезжает вправо при жесте "назад"
                    animationSpec = tween(slideDuration, easing = EaseIn)
                ) // Можно добавить + fadeOut()
            }
        ) {
            AISettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            NavRoutes.TimeSettings.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start, // Въезжает слева
                    animationSpec = tween(slideDuration, easing = EaseOut)
                ) // Можно добавить + fadeIn() для плавности
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start, // Уезжает влево (чуть медленнее/меньше, чтобы создать эффект глубины)
                    animationSpec = tween(slideDuration, easing = EaseIn),
                    targetOffset = { fullWidth -> -fullWidth / 4 } // Смещаем не полностью за экран сразу
                ) // Можно добавить + fadeOut()
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End, // Возвращается справа
                    animationSpec = tween(slideDuration, easing = EaseOut),
                    initialOffset = { fullWidth -> -fullWidth / 4 } // Начинает с той же позиции, куда уехал exit
                ) // Можно добавить + fadeIn()
            },
            popExitTransition = { // <-- САМАЯ ВАЖНАЯ для предиктивного жеста
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End, // Уезжает вправо при жесте "назад"
                    animationSpec = tween(slideDuration, easing = EaseIn)
                ) // Можно добавить + fadeOut()
            }
        ) {
            TimeSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                title = "Time & Format"
            )
        }
        composable(
            NavRoutes.Terms.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start, // Въезжает слева
                    animationSpec = tween(slideDuration, easing = EaseOut)
                ) // Можно добавить + fadeIn() для плавности
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start, // Уезжает влево (чуть медленнее/меньше, чтобы создать эффект глубины)
                    animationSpec = tween(slideDuration, easing = EaseIn),
                    targetOffset = { fullWidth -> -fullWidth / 4 } // Смещаем не полностью за экран сразу
                ) // Можно добавить + fadeOut()
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End, // Возвращается справа
                    animationSpec = tween(slideDuration, easing = EaseOut),
                    initialOffset = { fullWidth -> -fullWidth / 4 } // Начинает с той же позиции, куда уехал exit
                ) // Можно добавить + fadeIn()
            },
            popExitTransition = { // <-- САМАЯ ВАЖНАЯ для предиктивного жеста
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End, // Уезжает вправо при жесте "назад"
                    animationSpec = tween(slideDuration, easing = EaseIn)
                ) // Можно добавить + fadeOut()
            }
        ) {
            TermsOfUseScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                title = "Terms of Use"
            )
        }
        composable(
            route = "create_event/{initialDateEpochDay}",
            arguments = listOf(navArgument("initialDateEpochDay") { type = NavType.LongType })
        ) { backStackEntry ->
            val initialDateEpochDay = backStackEntry.arguments?.getLong("initialDateEpochDay") ?: LocalDate.now().toEpochDay()
            val initialDate = LocalDate.ofEpochDay(initialDateEpochDay)
            // Можно создать отдельную ViewModel или использовать общую MainViewModel
            CreateEventScreen(
                viewModel = hiltViewModel<MainViewModel>(), // Или CreateEventViewModel
                initialDate = initialDate,
                onNavigateBack = { navController.popBackStack() }
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