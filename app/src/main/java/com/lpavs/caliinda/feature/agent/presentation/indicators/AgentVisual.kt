package com.lpavs.caliinda.feature.agent.presentation.indicators

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.feature.agent.data.model.AgentState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AiVisualizer(
    aiState: AgentState,
    aiMessage: String?,
    modifier: Modifier = Modifier,
    onResultShownTimeout: () -> Unit,
    onAskingShownTimeout: () -> Unit
) {
  val targetState = aiState

  // --- LaunchedEffect для задержки перед IDLE ---
  LaunchedEffect(targetState) { // Запускается при смене targetState
    if (targetState == AgentState.RESULT) {
      delay(5000L) // Ждем 5 секунд
      onResultShownTimeout() // Вызываем колбэк после задержки
    }
  }

  LaunchedEffect(targetState) { // Запускается при смене targetState
    if (targetState == AgentState.ASKING) {
      delay(15000L) // Ждем 15 секунд
      onAskingShownTimeout() // Вызываем колбэк после задержки
    }
  }

  // --- 1. Animatable для вращения ---
  val rotationAngle = remember { Animatable(0f) }
  // LaunchedEffect для вращения остается без изменений...
  LaunchedEffect(targetState) {
    val shouldRotate = targetState == AgentState.THINKING || targetState == AgentState.LISTENING
    if (shouldRotate) {
      val duration =
          when (targetState) {
            AgentState.THINKING -> 7000
            AgentState.LISTENING -> 80000
            else -> 5000 // На всякий случай
          }
      if (!rotationAngle.isRunning &&
          (targetState == AgentState.THINKING || targetState == AgentState.LISTENING)) {
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
              AgentState.IDLE -> 0f
              AgentState.LISTENING -> 5f
              AgentState.THINKING -> 1f
              AgentState.ASKING -> 5f
              AgentState.RESULT -> 5f
              AgentState.ERROR -> 0f
            }
          }

  // --- Базовый размер ---
  val baseSizeDp = screenWidthDp * 0.4f

  val animatedOffsetYRatio by
      transition.animateFloat(
          transitionSpec = { spring(stiffness = Spring.StiffnessMedium) },
          label = "offsetYRatio") { state ->
            when (state) {
              AgentState.IDLE -> 0.6f
              AgentState.LISTENING -> 0.75f
              AgentState.THINKING -> 0.55f
              AgentState.ASKING,
              AgentState.RESULT -> 0.55f
              AgentState.ERROR -> 0.6f
            }
          }

  val animatedColor by
      transition.animateColor(transitionSpec = { tween(durationMillis = 500) }, label = "color") {
          state ->
        when (state) {
          AgentState.LISTENING -> colorScheme.primaryContainer
          AgentState.THINKING -> colorScheme.secondary
          AgentState.ASKING -> colorScheme.tertiary
          AgentState.RESULT -> colorScheme.primary
          else -> colorScheme.surface
        }
      }

  // Видимость всего компонента
  val isComponentVisible = targetState != AgentState.IDLE && targetState != AgentState.ERROR

  // Видимость бабла
  val isBubbleVisible =
      (targetState == AgentState.ASKING || targetState == AgentState.RESULT) &&
          !aiMessage.isNullOrEmpty()

  // Общий контейнер для позиционирования по Y и анимации появления/исчезания
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
        // Box для позиционирования звезды и бабла
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.fillMaxSize().offset {
                  IntOffset(x = 0, y = (animatedOffsetYRatio * screenHeightPx / 2).toInt())
                }) {
              // --- 1. Анимированная Звезда (под баблом) ---
              Box(
                  modifier =
                      Modifier.size(baseSizeDp).aspectRatio(1f).graphicsLayer {
                        scaleX = animatedScaleFactor
                        scaleY = animatedScaleFactor
                        rotationZ = rotationAngle.value
                        transformOrigin = TransformOrigin.Center
                      }) {
                    Surface(
                        modifier = Modifier.matchParentSize().clip(AiStarShape),
                        color = animatedColor,
                    ) {}
                  }

              // --- 2. Бабл с текстом (поверх звезды) ---
              AnimatedVisibility(
                  visible = isBubbleVisible,
                  enter =
                      fadeIn(animationSpec = tween(delayMillis = 150, durationMillis = 300)) +
                          scaleIn(
                              initialScale = 0.8f,
                              transformOrigin = TransformOrigin(0.5f, 0.5f),
                              animationSpec = tween(delayMillis = 150, durationMillis = 300)),
                  exit =
                      fadeOut(animationSpec = tween(durationMillis = 150)) +
                          scaleOut(
                              targetScale = 0.8f,
                              transformOrigin = TransformOrigin(0.5f, 0.5f),
                              animationSpec = tween(durationMillis = 150)),
                  modifier =
                      Modifier.align(Alignment.Center)
                          .widthIn(max = baseSizeDp * 2f)
                          .wrapContentHeight()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colorScheme.tertiaryContainer,
                        tonalElevation = 2.dp,
                        modifier = Modifier.padding(horizontal = 1.dp, vertical = 2.dp)) {
                          Text(
                              text = aiMessage ?: "",
                              style = typography.bodyMedium,
                              textAlign = TextAlign.Center,
                              color = colorScheme.onTertiaryContainer,
                              modifier =
                                  Modifier.padding(
                                      horizontal = 16.dp,
                                      vertical = 12.dp) // Добавил padding для текста
                              )
                        }
                  }
            }
      }
}
