package com.example.caliindar.ui.screens.main.components.AI

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val targetState = aiState // Target state for animations

    // --- 1. Animatable для вращения (остается без изменений) ---
    val rotationAngle = remember { Animatable(0f) }
    LaunchedEffect(targetState) {
        val shouldRotate = targetState == AiVisualizerState.THINKING || targetState == AiVisualizerState.RECORDING
        if (shouldRotate) {
            val duration = when (targetState) {
                AiVisualizerState.THINKING -> 7000
                AiVisualizerState.RECORDING -> 80000
                else -> 5000
            }
            launch {
                rotationAngle.animateTo(
                    targetValue = rotationAngle.value + 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = duration, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
            }
        } else {
            if (rotationAngle.isRunning || rotationAngle.value % 360f != 0f) {
                launch {
                    rotationAngle.stop()
                    rotationAngle.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 500, easing = EaseOutCubic)
                    )
                }
            }
        }
    }

    // --- 3. Transition для управления остальными свойствами ---
    val transition = updateTransition(targetState = targetState, label = "AiVisualizerTransition")

    // --- Анимированные свойства ---
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightPx = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }

    // --- НОВОЕ: Анимированный Фактор Масштаба ---
    val animatedScaleFactor by transition.animateFloat(
        transitionSpec = { spring(stiffness = Spring.StiffnessMediumLow) },
        label = "scaleFactor"
    ) { state ->
        when (state) {
            AiVisualizerState.IDLE -> 0f
            AiVisualizerState.RECORDING -> 5f // Теперь это масштаб
            AiVisualizerState.THINKING -> 1f
            AiVisualizerState.ASKING -> 5f
            AiVisualizerState.RESULT -> 5f
            AiVisualizerState.ERROR -> 0f
        }
    }

    // --- Базовый размер (например, размер в состоянии THINKING) ---
    // Вы можете выбрать любой разумный базовый размер, который затем будет масштабироваться.
    // Например, 40% ширины экрана, как в состоянии THINKING.
    val baseSizeDp = screenWidthDp * 0.4f

    val animatedOffsetYRatio by transition.animateFloat(
        transitionSpec = { spring(stiffness = Spring.StiffnessMedium) },
        label = "offsetYRatio"
    ) { state ->
        when (state) {
            AiVisualizerState.IDLE -> 0.6f
            AiVisualizerState.RECORDING -> 0.75f
            AiVisualizerState.THINKING -> 0f
            AiVisualizerState.ASKING, AiVisualizerState.RESULT -> 0.55f
            AiVisualizerState.ERROR -> 0.6f
        }
    }

    val animatedColor by transition.animateColor(
        transitionSpec = { tween(durationMillis = 500) },
        label = "color"
    ) { state ->
        when (state) {
            AiVisualizerState.RECORDING -> MaterialTheme.colorScheme.primary
            AiVisualizerState.THINKING -> MaterialTheme.colorScheme.secondary
            AiVisualizerState.ASKING -> MaterialTheme.colorScheme.tertiary
            AiVisualizerState.RESULT -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        }
    }
    // --- Конец анимированных свойств ---

    // Используем animatedScaleFactor для определения видимости
    val isVisible = targetState != AiVisualizerState.IDLE && animatedScaleFactor > 0f

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(durationMillis = 400, easing = EaseOutCubic)
        ) + fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = slideOutVertically(
            targetOffsetY = { it / 2 },
            animationSpec = tween(durationMillis = 300, easing = EaseInCubic)
        ) + fadeOut(animationSpec = tween(durationMillis = 200)),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        x = 0,
                        y = (animatedOffsetYRatio * screenHeightPx / 2).toInt()
                    )
                }
        ) {
            Surface(
                modifier = Modifier
                    // --- Устанавливаем базовый размер для лэйаута ---
                    .size(baseSizeDp)
                    .aspectRatio(1f)
                    // --- Применяем масштабирование и вращение через graphicsLayer ---
                    .graphicsLayer {
                        scaleX = animatedScaleFactor // << Масштаб по X
                        scaleY = animatedScaleFactor // << Масштаб по Y
                        rotationZ = rotationAngle.value // << Вращение
                        // Можно установить transformOrigin здесь, если нужно масштабировать/вращать
                        // не относительно центра по умолчанию
                        // transformOrigin = TransformOrigin.Center
                    }
                    .clip(AiStarShape), // Ваша форма
                color = animatedColor, // Цвет из transition
            ) {
                // Содержимое Surface (текст и т.д.) будет масштабироваться вместе с Surface
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        // Добавляем graphicsLayer с обратным масштабом к тексту,
                        // если НЕ хотим, чтобы текст масштабировался вместе с формой.
                        // Если текст ДОЛЖЕН масштабироваться, этот graphicsLayer не нужен.
                        .graphicsLayer {
                            // Раскомментируйте, если текст не должен масштабироваться
                            scaleX = 1f / animatedScaleFactor.coerceAtLeast(0.01f) // Деление на 0!
                            scaleY = 1f / animatedScaleFactor.coerceAtLeast(0.01f)
                        }
                        .padding(16.dp) // Паддинг будет применен к немасштабированному размеру
                ) {
                    AnimatedVisibility(
                        visible = (targetState == AiVisualizerState.ASKING || targetState == AiVisualizerState.RESULT) && aiMessage != null,
                        enter = fadeIn(animationSpec = tween(delayMillis = 300)),
                        exit = fadeOut()
                    ) {
                        Box(

                        ){
                            Text(
                                text = aiMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = contentColorFor(animatedColor)
                            )
                        }
                        /*
                        ClickableLinkText(
                            text = aiMessage ?: "",
                            uriHandler = uriHandler
                        )
                         */
                    }
                }
            }
        }
    }
}