package com.lpavs.caliinda.feature.agent.presentation.indicators

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.feature.agent.data.model.AgentState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AiVisualizer(
    aiState: AgentState,
    modifier: Modifier = Modifier,
) {
  val targetState = aiState

  // --- 1. Animatable для вращения ---
  val rotationAngle = remember { Animatable(0f) }
  // LaunchedEffect для вращения остается без изменений...
  LaunchedEffect(targetState) {
    val shouldRotate = targetState == AgentState.LISTENING
    if (shouldRotate) {
      val duration =
          when (targetState) {
            AgentState.LISTENING -> 80000
            else -> 5000 // На всякий случай
          }
      if (!rotationAngle.isRunning) {
        launch {
          rotationAngle.animateTo(
              targetValue = rotationAngle.value + 360f,
              animationSpec =
                  infiniteRepeatable(
                      animation = tween(durationMillis = duration, easing = LinearEasing),
                      repeatMode = RepeatMode.Restart))
        }
      }
    } else {
      if (rotationAngle.isRunning) {
        launch {
          rotationAngle.stop()
          rotationAngle.value
          rotationAngle.animateTo(
              targetValue = 0f, // Возвращаем в 0 для предсказуемости
              animationSpec = tween(durationMillis = 500, easing = EaseOutCubic))
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
  val animatedScaleFactor by
      transition.animateFloat(
          transitionSpec = { spring(stiffness = Spring.StiffnessMediumLow) },
          label = "scaleFactor") { state ->
            when (state) {
              AgentState.LISTENING -> 5f
              else -> 0f
            }
          }

  // --- Базовый размер ---
  val baseSizeDp = screenWidthDp * 0.4f

  val animatedOffsetYRatio by
      transition.animateFloat(
          transitionSpec = { spring(stiffness = Spring.StiffnessMedium) },
          label = "offsetYRatio") { state ->
            when (state) {
              AgentState.LISTENING -> 0.75f
              AgentState.THINKING -> 0.75f
              else -> 0f
            }
          }

  val animatedColor by
      transition.animateColor(transitionSpec = { tween(durationMillis = 500) }, label = "color") {
          state ->
        when (state) {
          AgentState.LISTENING -> colorScheme.primaryContainer
          else -> colorScheme.surface
        }
      }

  // Видимость всего компонента
  val isComponentVisible = targetState != AgentState.IDLE && targetState != AgentState.ERROR
  // Общий контейнер для позиционирования по Y и анимации появления/исчезания

    val breathingAmplitude: Dp = 300.dp
    val animationDurationMillis = 650
    val infiniteTransition = rememberInfiniteTransition(label = "breathing_transition")

    val emojis = listOf("🤔",  "🔎", "🧐", "🗓️")
    var currentEmojiIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(key1 = true) {
        while (true) {
            delay(animationDurationMillis.toLong() *2)
            currentEmojiIndex = (currentEmojiIndex + 1) % emojis.size
        }
    }
    val breathingOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -breathingAmplitude.value,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDurationMillis, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_offset_y"
    )
  AnimatedVisibility(
      visible = isComponentVisible,
      enter =
          slideInVertically(
              initialOffsetY = { it / 2 },
              animationSpec = tween(durationMillis = 400, easing = EaseOutCubic)) +
              fadeIn(animationSpec = tween(durationMillis = 300)),
      exit =
          slideOutVertically(
              targetOffsetY = { it / 2 },
              animationSpec = tween(durationMillis = 300, easing = EaseInCubic)) +
              fadeOut(animationSpec = tween(durationMillis = 200)),
      modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(x = 0, y = (animatedOffsetYRatio * screenHeightPx / 2).toInt())
                    }) {
              // --- 1. Анимированная Звезда (под баблом) ---
              Box(
                  modifier =
                      Modifier
                          .size(baseSizeDp)
                          .aspectRatio(1f)
                          .graphicsLayer {
                              scaleX = animatedScaleFactor
                              scaleY = animatedScaleFactor
                              rotationZ = rotationAngle.value
                              transformOrigin = TransformOrigin.Center
                          }) {
                    Surface(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(AiStarShape),
                        color = animatedColor,
                    ) {}
                  }
            }
      }
  AnimatedVisibility(
      visible = (targetState == AgentState.THINKING),
      enter =
          slideInVertically(
              initialOffsetY = { it / 2 },
              animationSpec = tween(durationMillis = 400, easing = EaseOutCubic)) +
              fadeIn(animationSpec = tween(durationMillis = 300)),
      exit =
          slideOutVertically(
              targetOffsetY = { it / 2 },
              animationSpec = tween(durationMillis = 300, easing = EaseInCubic)) +
              fadeOut(animationSpec = tween(durationMillis = 200)),
      modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(x = 0, y = (animatedOffsetYRatio * screenHeightPx / 3.5).toInt())
                    }) {
              // --- 1. Анимированная Звезда (под баблом) ---
            LoadingIndicator(
                modifier = Modifier
                    .size(200.dp)
                    .offset { IntOffset(x = 0, y = breathingOffsetY.toInt()) }
            )
            Text(text = emojis[currentEmojiIndex], modifier = Modifier.offset { IntOffset(x = 0, y = breathingOffsetY.toInt()) })
            }
      }
}
