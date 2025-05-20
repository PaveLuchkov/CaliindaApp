package com.lpavs.caliinda.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lpavs.caliinda.ui.screens.settings.SettingsScreen
import com.lpavs.caliinda.ui.screens.main.MainScreen
import com.lpavs.caliinda.ui.screens.main.MainViewModel
import com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.CreateEventScreen
import com.lpavs.caliinda.ui.screens.settings.AISettingsScreen
import com.lpavs.caliinda.ui.screens.settings.TermsOfUseScreen
import com.lpavs.caliinda.ui.screens.settings.TimeSettingsScreen
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
                fadeIn(
                    animationSpec = tween(slideDuration, easing = EaseOut),
                    initialAlpha = 0.6f
                )
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
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
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
//        composable(
//            route = "create_event/{initialDateEpochDay}", // Оставьте ваш шаблон роута как есть
//            arguments = listOf(navArgument("initialDateEpochDay") { type = NavType.LongType }),
//            enterTransition = {
//                slideIntoContainer(
//                    towards = AnimatedContentTransitionScope.SlideDirection.Up, // Въезжает слева
//                    animationSpec = tween(slideDuration, easing = EaseOut)
//                ) // Можно добавить + fadeIn() для плавности
//            },
//            exitTransition = {
//                slideOutOfContainer(
//                    towards = AnimatedContentTransitionScope.SlideDirection.Up, // Уезжает влево (чуть медленнее/меньше, чтобы создать эффект глубины)
//                    animationSpec = tween(slideDuration, easing = EaseIn),
//                    targetOffset = { fullWidth -> -fullWidth / 4 } // Смещаем не полностью за экран сразу
//                ) // Можно добавить + fadeOut()
//            },
//            popEnterTransition = {
//                slideIntoContainer(
//                    towards = AnimatedContentTransitionScope.SlideDirection.Down, // Возвращается справа
//                    animationSpec = tween(slideDuration, easing = EaseOut),
//                    initialOffset = { fullWidth -> -fullWidth / 4 } // Начинает с той же позиции, куда уехал exit
//                ) // Можно добавить + fadeIn()
//            },
//            popExitTransition = { // <-- САМАЯ ВАЖНАЯ для предиктивного жеста
//                slideOutOfContainer(
//                    towards = AnimatedContentTransitionScope.SlideDirection.Down, // Уезжает вправо при жесте "назад"
//                    animationSpec = tween(slideDuration, easing = EaseIn)
//                ) // Можно добавить + fadeOut()
//            }
//        ) { backStackEntry ->
//            val initialDateEpochDay = backStackEntry.arguments?.getLong("initialDateEpochDay") ?: LocalDate.now().toEpochDay()
//            val initialDate = LocalDate.ofEpochDay(initialDateEpochDay)
//            CreateEventScreen(
//                viewModel = viewModel, // ПРАВИЛЬНО: Используем экземпляр, переданный в AppNavHost
//                initialDate = initialDate,
//                onNavigateBack = { navController.popBackStack() }
//            )
//        }
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