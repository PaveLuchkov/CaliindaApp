package com.example.caliindar.ui.screens.main.components.AI

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun AiVisualizer(
    aiState: AiVisualizerState,
    aiMessage: String?,
    modifier: Modifier = Modifier,
    onResultShownTimeout: () -> Unit,
    onAskingShownTimeout: () -> Unit
) {
    val targetState = aiState

    // --- LaunchedEffect для задержки перед IDLE ---
    LaunchedEffect(targetState) { // Запускается при смене targetState
        if (targetState == AiVisualizerState.RESULT) {
            delay(5000L) // Ждем 5 секунд
            onResultShownTimeout() // Вызываем колбэк после задержки
        }
    }

    LaunchedEffect(targetState) { // Запускается при смене targetState
        if (targetState == AiVisualizerState.ASKING) {
            delay(15000L) // Ждем 15 секунд
            onAskingShownTimeout() // Вызываем колбэк после задержки
        }
    }

    // --- 1. Animatable для вращения ---
    val rotationAngle = remember { Animatable(0f) }
    // LaunchedEffect для вращения остается без изменений...
    LaunchedEffect(targetState) {
        val shouldRotate = targetState == AiVisualizerState.THINKING || targetState == AiVisualizerState.LISTENING
        if (shouldRotate) {
            val duration = when (targetState) {
                AiVisualizerState.THINKING -> 7000
                AiVisualizerState.LISTENING -> 80000
                else -> 5000 // На всякий случай
            }
            if (!rotationAngle.isRunning && (targetState == AiVisualizerState.THINKING || targetState == AiVisualizerState.LISTENING)) {
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
            if (rotationAngle.isRunning) {
                launch {
                    rotationAngle.stop()
                    val currentRotation = rotationAngle.value
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

    // --- Анимированные свойства ---
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightPx = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }

    // --- Анимированный Фактор Масштаба ---
    val animatedScaleFactor by transition.animateFloat(
        transitionSpec = { spring(stiffness = Spring.StiffnessMediumLow) },
        label = "scaleFactor"
    ) { state ->
        when (state) {
            AiVisualizerState.IDLE -> 0f
            AiVisualizerState.LISTENING -> 5f
            AiVisualizerState.THINKING -> 1f
            AiVisualizerState.ASKING -> 5f
            AiVisualizerState.RESULT -> 5f
            AiVisualizerState.ERROR -> 0f
        }
    }

    // --- Базовый размер ---
    val baseSizeDp = screenWidthDp * 0.4f

    val animatedOffsetYRatio by transition.animateFloat(
        transitionSpec = { spring(stiffness = Spring.StiffnessMedium) },
        label = "offsetYRatio"
    ) { state ->
        when (state) {
            AiVisualizerState.IDLE -> 0.6f
            AiVisualizerState.LISTENING -> 0.75f
            AiVisualizerState.THINKING -> 0.55f
            AiVisualizerState.ASKING, AiVisualizerState.RESULT -> 0.55f
            AiVisualizerState.ERROR -> 0.6f
        }
    }

    val animatedColor by transition.animateColor(
        transitionSpec = { tween(durationMillis = 500) },
        label = "color"
    ) { state ->
        when (state) {
            AiVisualizerState.LISTENING -> colorScheme.primaryContainer
            AiVisualizerState.THINKING -> colorScheme.secondary
            AiVisualizerState.ASKING -> colorScheme.tertiary
            AiVisualizerState.RESULT -> colorScheme.primary
            else -> colorScheme.surface
        }
    }

    // Видимость всего компонента
    val isComponentVisible = targetState != AiVisualizerState.IDLE && targetState != AiVisualizerState.ERROR

    // Видимость бабла
    val isBubbleVisible = (targetState == AiVisualizerState.ASKING || targetState == AiVisualizerState.RESULT) && !aiMessage.isNullOrEmpty()

    // Общий контейнер для позиционирования по Y и анимации появления/исчезания
    AnimatedVisibility(
        visible = isComponentVisible,
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
        // Box для позиционирования звезды и бабла
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
            // --- 1. Анимированная Звезда (под баблом) ---
            Box(
                modifier = Modifier
                    .size(baseSizeDp)
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
                        .matchParentSize()
                        .clip(AiStarShape),
                    color = animatedColor,
                ) {}
            }

            // --- 2. Бабл с текстом (поверх звезды) ---
            AnimatedVisibility(
                visible = isBubbleVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 150, durationMillis = 300)) +
                        scaleIn(
                            initialScale = 0.8f,
                            transformOrigin = TransformOrigin(0.5f, 0.5f),
                            animationSpec = tween(delayMillis = 150, durationMillis = 300)
                        ),
                exit = fadeOut(animationSpec = tween(durationMillis = 150)) +
                        scaleOut(
                            targetScale = 0.8f,
                            transformOrigin = TransformOrigin(0.5f, 0.5f),
                            animationSpec = tween(durationMillis = 150)
                        ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = baseSizeDp * 2f)
                    .wrapContentHeight()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.tertiaryContainer,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .padding(horizontal = 1.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = aiMessage ?: "",
                        style = typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp) // Добавил padding для текста
                    )
                }

            }
        }
    }
}