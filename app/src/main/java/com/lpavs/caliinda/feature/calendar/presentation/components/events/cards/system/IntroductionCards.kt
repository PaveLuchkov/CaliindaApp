@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.SwipeRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.theme.AppMotion
import com.lpavs.caliinda.core.ui.theme.CaliindaFonts
import com.lpavs.caliinda.core.ui.theme.Typography
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.CalendarEventItem
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.CalendarProjectItem
import com.lpavs.caliinda.feature.calendar.presentation.components.events.lists.ProjectRibbons
import com.lpavs.caliinda.feature.calendar.presentation.intro.IntroSamples
import com.lpavs.caliinda.feature.calendar.presentation.model.EventUiModel

/**
 * Знакомство прямо в ленте дня: карточка с пояснением шага, а под ней — живые примеры теми же
 * компонентами, что и настоящие события.
 */
@Composable
fun IntroductionEvent(
    state: IntroState,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onCreateEvent: () -> Unit,
) {
  val step = state.currentStep
  Column {
    IntroCard(step = step, onNext = onNext, onSkip = onSkip, onCreateEvent = onCreateEvent)
    val samples = state.samples ?: return@Column
    AnimatedContent(
        targetState = step,
        transitionSpec = {
          fadeIn(AppMotion.defaultEffectsSpec()) togetherWith fadeOut(AppMotion.fastEffectsSpec())
        },
        label = "introDemo") { shownStep ->
          IntroDemo(shownStep, samples)
        }
  }
}

/** Пояснение шага: точки прогресса, заголовок, текст и кнопки. */
@Composable
private fun IntroCard(
    step: IntroStep,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onCreateEvent: () -> Unit,
) {
  val isLast = step == IntroStep.START
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = cuid.ItemHorizontalPadding, vertical = cuid.ItemVerticalPadding)
              .background(colorScheme.surfaceContainer, RoundedCornerShape(cuid.ContainerCornerRadius))
              .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp)) {
        StepDots(current = step)
        AnimatedContent(
            targetState = step,
            transitionSpec = {
              fadeIn(AppMotion.defaultEffectsSpec()) togetherWith fadeOut(AppMotion.fastEffectsSpec())
            },
            label = "introText") { shownStep ->
              Column(modifier = Modifier.padding(top = 12.dp, end = 8.dp)) {
                Text(
                    text = stringResource(shownStep.titleRes),
                    style = Typography.headlineSmallEmphasized,
                    fontFamily = CaliindaFonts.Card,
                    color = colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(shownStep.textRes),
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant)
                if (shownStep == IntroStep.PROJECTS) SwipeHint()
              }
            }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically) {
              if (isLast) {
                TextButton(onClick = onNext) { Text(stringResource(R.string.intro_finish)) }
                Button(onClick = onCreateEvent) {
                  Icon(Icons.Rounded.Add, contentDescription = null)
                  Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                  Text(stringResource(R.string.intro_create_event))
                }
              } else {
                TextButton(onClick = onSkip) { Text(stringResource(R.string.intro_skip)) }
                Button(onClick = onNext) { Text(stringResource(R.string.intro_next)) }
              }
            }
      }
}

/** Прогресс по шагам: текущий — вытянутая капсула, остальные — точки. */
@Composable
private fun StepDots(current: IntroStep) {
  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    IntroStep.entries.forEach { step ->
      val active = step == current
      val width by
          animateDpAsState(if (active) 20.dp else 8.dp, AppMotion.defaultSpatialSpec(), label = "dot")
      Box(
          modifier =
              Modifier.height(8.dp)
                  .width(width)
                  .background(
                      if (step.ordinal <= current.ordinal) colorScheme.primary
                      else colorScheme.outlineVariant,
                      CircleShape))
    }
  }
}

/** Подсказка жеста: иконка свайпа покачивается вправо, туда, куда вести палец. */
@Composable
private fun SwipeHint() {
  val shift by
      rememberInfiniteTransition(label = "swipeHint")
          .animateFloat(
              initialValue = 0f,
              targetValue = 1f,
              animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
              label = "swipeShift")
  Icon(
      Icons.Rounded.SwipeRight,
      contentDescription = null,
      tint = colorScheme.primary,
      modifier =
          Modifier.padding(top = 8.dp).size(28.dp).graphicsLayer {
            translationX = shift * 12.dp.toPx()
          })
}

/** Примеры под карточкой — как обычные элементы ленты, без действий. */
@Composable
private fun IntroDemo(step: IntroStep, samples: IntroSamples) {
  Column(modifier = Modifier.padding(top = 4.dp)) {
    when (step) {
      IntroStep.COLORS -> samples.timeline.forEach { DemoEvent(it) }
      IntroStep.HEIGHT -> samples.durations.forEach { DemoEvent(it) }
      IntroStep.PROJECTS -> {
        ProjectRibbons(ribbons = listOf(samples.ribbon), onClick = {})
        Spacer(Modifier.height(4.dp))
        CalendarProjectItem(
            uiModel = samples.project,
            isExpanded = false,
            onToggleExpand = {},
            onDetailsClickFromList = {},
            onDeleteClickFromList = {},
            onEditClickFromList = {})
      }
      IntroStep.START -> {}
    }
  }
}

@Composable
private fun DemoEvent(uiModel: EventUiModel) {
  CalendarEventItem(
      uiModel = uiModel,
      isExpanded = false,
      onToggleExpand = {},
      onDetailsClickFromList = {},
      onDeleteClickFromList = {},
      onEditClickFromList = {})
}

/** Подсказка на экране проектов, пока знакомство не пройдено. */
@Composable
fun IntroductionProjectsHint() {
  Text(
      text = stringResource(R.string.intro_projects_page),
      style = typography.bodyLarge,
      color = colorScheme.onSurfaceVariant,
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = cuid.ItemHorizontalPadding, vertical = cuid.ItemVerticalPadding)
              .background(colorScheme.surfaceContainer, RoundedCornerShape(cuid.ContainerCornerRadius))
              .padding(20.dp))
}

private val IntroStep.titleRes: Int
  get() =
      when (this) {
        IntroStep.COLORS -> R.string.intro_colors_title
        IntroStep.HEIGHT -> R.string.intro_height_title
        IntroStep.PROJECTS -> R.string.intro_projects_title
        IntroStep.START -> R.string.intro_start_title
      }

private val IntroStep.textRes: Int
  get() =
      when (this) {
        IntroStep.COLORS -> R.string.intro_colors_text
        IntroStep.HEIGHT -> R.string.intro_height_text
        IntroStep.PROJECTS -> R.string.intro_projects_text
        IntroStep.START -> R.string.intro_start_text
      }
