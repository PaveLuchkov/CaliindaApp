package com.example.caliindar.ui.screens.main.components.AI

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import com.example.caliindar.ui.screens.main.components.AI.AiStarShape // Import your shape
import com.example.caliindar.ui.common.ClickableLinkText // Keep if needed for result text
import com.example.caliindar.ui.screens.main.AiVisualizerState
import kotlinx.coroutines.launch


@Composable
fun AiVisualizer(
    aiState: AiVisualizerState,
    aiMessage: String?,
    modifier: Modifier = Modifier,
    uriHandler: UriHandler // Pass if needed for links
) {
    val targetState = aiState

    val BubbleBackgroundColor = colorScheme.secondaryContainer
    val BubbleTextColor = colorScheme.onSecondaryContainer

    // --- 1. Animatable для вращения (как в исходном коде) ---
    val rotationAngle = remember { Animatable(0f) }
    LaunchedEffect(targetState) {
        val shouldRotate = targetState == AiVisualizerState.THINKING || targetState == AiVisualizerState.RECORDING
        if (shouldRotate) {
            val duration = when (targetState) {
                AiVisualizerState.THINKING -> 7000
                AiVisualizerState.RECORDING -> 80000
                else -> 5000 // На всякий случай
            }
            // Запускаем только если еще не запущена и состояние соответствующее
            if (!rotationAngle.isRunning && (targetState == AiVisualizerState.THINKING || targetState == AiVisualizerState.RECORDING)) {
                launch {
                    rotationAngle.animateTo(
                        targetValue = rotationAngle.value + 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = duration, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )
                }
            }
        } else {
            // Останавливаем плавно, если вращается
            if (rotationAngle.isRunning) {
                launch {
                    rotationAngle.stop()
                    val currentRotation = rotationAngle.value
                    // Можно вернуть в 0 или оставить как есть
                    rotationAngle.animateTo(
                        targetValue = 0f, // Возвращаем в 0 для предсказуемости
                        animationSpec = tween(durationMillis = 500, easing = EaseOutCubic)
                    )
                }
            }
        }
    }


    // --- 3. Transition для управления остальными свойствами ---
    val transition = updateTransition(targetState = targetState, label = "AiVisualizerTransition")

    // --- Анимированные свойства (ВОССТАНОВЛЕНЫ ИЗ ИСХОДНОГО КОДА) ---
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightPx = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }

    // --- Анимированный Фактор Масштаба (как в исходном коде) ---
    val animatedScaleFactor by transition.animateFloat(
        transitionSpec = { spring(stiffness = Spring.StiffnessMediumLow) },
        label = "scaleFactor"
    ) { state ->
        when (state) {
            AiVisualizerState.IDLE -> 0f
            AiVisualizerState.RECORDING -> 5f // Большой масштаб
            AiVisualizerState.THINKING -> 1f // Базовый масштаб
            AiVisualizerState.ASKING -> 5f // Большой масштаб
            AiVisualizerState.RESULT -> 5f // Большой масштаб
            AiVisualizerState.ERROR -> 0f
        }
    }

    // --- Базовый размер (как в исходном коде) ---
    val baseSizeDp = screenWidthDp * 0.4f

    val animatedOffsetYRatio by transition.animateFloat(
        transitionSpec = { spring(stiffness = Spring.StiffnessMedium) },
        label = "offsetYRatio"
    ) { state ->
        when (state) {
            // Смещения как в исходном коде
            AiVisualizerState.IDLE -> 0.6f
            AiVisualizerState.RECORDING -> 0.75f // Сильно вниз
            AiVisualizerState.THINKING -> 0.55f // Стандартное положение
            AiVisualizerState.ASKING, AiVisualizerState.RESULT -> 0.55f // Такое же как THINKING
            AiVisualizerState.ERROR -> 0.6f
        }
    }

    val animatedColor by transition.animateColor(
        transitionSpec = { tween(durationMillis = 500) },
        label = "color"
    ) { state ->
        // Цвета звезды как в исходном коде
        when (state) {
            AiVisualizerState.RECORDING -> MaterialTheme.colorScheme.primaryContainer
            AiVisualizerState.THINKING -> MaterialTheme.colorScheme.secondary
            AiVisualizerState.ASKING -> MaterialTheme.colorScheme.tertiary // Цвет для ASKING
            AiVisualizerState.RESULT -> MaterialTheme.colorScheme.primary // Цвет для RESULT
            else -> MaterialTheme.colorScheme.surface // Для IDLE/ERROR
        }
    }
    // --- Конец анимированных свойств ---

    // Видимость всего компонента
    val isComponentVisible = targetState != AiVisualizerState.IDLE && targetState != AiVisualizerState.ERROR

    // Видимость бабла
    val isBubbleVisible = (targetState == AiVisualizerState.ASKING || targetState == AiVisualizerState.RESULT) && !aiMessage.isNullOrEmpty()

    // Общий контейнер для позиционирования по Y и анимации появления/исчезания
    AnimatedVisibility(
        visible = isComponentVisible,
        enter = slideInVertically(
            initialOffsetY = { it / 2 }, // Появляется сверху
            animationSpec = tween(durationMillis = 400, easing = EaseOutCubic)
        ) + fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = slideOutVertically(
            targetOffsetY = { it / 2 }, // Уезжает вверх
            animationSpec = tween(durationMillis = 300, easing = EaseInCubic)
        ) + fadeOut(animationSpec = tween(durationMillis = 200)),
        modifier = modifier
    ) {
        // Box для позиционирования звезды и бабла
        // и для применения общего смещения по Y
        Box(
            // Центрируем содержимое (звезду и бабл) внутри этого Box
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        x = 0,
                        // Применяем общее смещение по Y из transition
                        y = (animatedOffsetYRatio * screenHeightPx / 2).toInt()
                    )
                }
        ) {
            // --- 1. Анимированная Звезда (под баблом) ---
            // Box для управления размером, масштабом и вращением звезды
            Box(
                modifier = Modifier
                    .size(baseSizeDp) // Базовый размер
                    .aspectRatio(1f)
                    .graphicsLayer {
                        scaleX = animatedScaleFactor
                        scaleY = animatedScaleFactor
                        rotationZ = rotationAngle.value
                        transformOrigin = TransformOrigin.Center
                    }
            ) {
                Surface(
                    modifier = Modifier
                        .matchParentSize() // Заполняем Box
                        .clip(AiStarShape), // Используем твою форму
                    color = animatedColor, // Анимированный цвет звезды
                ) {
                }
            }

            // --- 2. Бабл с текстом (поверх звезды) ---
            AnimatedVisibility(
                visible = isBubbleVisible,
                // Анимации появления/исчезания бабла
                enter = fadeIn(animationSpec = tween(delayMillis = 150, durationMillis = 300)) +
                        scaleIn( // Появляется с небольшим увеличением
                            initialScale = 0.8f,
                            transformOrigin = TransformOrigin(0.5f, 0.5f), // Масштаб от центра
                            animationSpec = tween(delayMillis = 150, durationMillis = 300)
                        ),
                exit = fadeOut(animationSpec = tween(durationMillis = 150)) +
                        scaleOut( // Исчезает с небольшим уменьшением
                            targetScale = 0.8f,
                            transformOrigin = TransformOrigin(0.5f, 0.5f),
                            animationSpec = tween(durationMillis = 150)
                        ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = baseSizeDp * 2f) // Не шире 90% от базового размера звезды
                    // .widthIn(max = screenWidthDp * 0.75f)
                    .wrapContentHeight() // Высота по содержимому
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp), // Скругление углов бабла
                    color = BubbleBackgroundColor, // Используем кастомный цвет фона
                    tonalElevation = 2.dp, // Небольшая тень для отделения
                    modifier = Modifier
                        .padding(horizontal = 1.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = aiMessage ?: "", // Безопасно отображаем текст
                        style = MaterialTheme.typography.headlineSmall, // Можно настроить стиль
                        textAlign = TextAlign.Center,
                        color = BubbleTextColor
                    )
                    /*
                    ClickableLinkText(
                        text = aiMessage ?: "",
                        uriHandler = uriHandler,
                        )

                     */
                }
            }
        }
    }
}