@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.lpavs.caliinda.feature.event_management.ui.shared.sections

import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.theme.AppMotion
import com.lpavs.caliinda.feature.event_management.ui.shared.ConnectedToggle
import com.lpavs.caliinda.feature.event_management.ui.shared.ConnectedToggles

/**
 * Необязательное: место, описание, напоминание. Сверху — группа кнопок, каждая открывает
 * только своё; заполненное видно сразу. Напоминание выбирается в диалоге ([onReminderClick]).
 */
@Composable
fun DetailsSection(
    location: String,
    onLocationChange: (String) -> Unit,
    /** Состояние поля, а не строка: длинное описание не перерисовывает форму на каждую букву. */
    description: TextFieldState,
    reminderMinutes: Int?,
    isAllDay: Boolean,
    onReminderClick: () -> Unit,
    enabled: Boolean,
    /** Поле места — по умолчанию с подсказками адресов ([LocationField]). */
    locationField: @Composable (FocusRequester) -> Unit = { focus ->
      LocationField(
          location = location,
          onLocationChange = onLocationChange,
          enabled = enabled,
          focusRequester = focus)
    },
) {
  // Открытое кнопкой поле; заполненное видно всегда — скрыть его, не стерев текст, нельзя.
  var placeOpen by rememberSaveable { mutableStateOf(false) }
  var notesOpen by rememberSaveable { mutableStateOf(false) }
  val showPlace = placeOpen || location.isNotEmpty()
  // derivedStateOf: перерисовываемся, только когда описание стало пустым или непустым.
  val hasNotes by remember(description) { derivedStateOf { description.text.isNotEmpty() } }
  val showNotes = notesOpen || hasNotes
  val placeFocus = remember { FocusRequester() }
  val notesFocus = remember { FocusRequester() }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    ConnectedToggles(
        toggles =
            listOf(
                ConnectedToggle(stringResource(R.string.details_place), showPlace, Icons.Rounded.Place) {
                  if (location.isEmpty()) placeOpen = !placeOpen
                },
                ConnectedToggle(stringResource(R.string.details_notes), showNotes, Icons.Rounded.Notes) {
                  if (!hasNotes) notesOpen = !notesOpen
                },
                ConnectedToggle(
                    reminderShortLabel(reminderMinutes, isAllDay),
                    reminderMinutes != null,
                    if (reminderMinutes != null) Icons.Rounded.Notifications
                    else Icons.Rounded.NotificationsOff) {
                  onReminderClick()
                }),
        enabled = enabled)

    AnimatedVisibility(
        visible = showPlace,
        enter = fadeIn(AppMotion.fastEffectsSpec()) + expandVertically(AppMotion.defaultSpatialSpec()),
        exit = fadeOut(AppMotion.fastEffectsSpec()) + shrinkVertically(AppMotion.defaultSpatialSpec())) {
          locationField(placeFocus)
        }
    AnimatedVisibility(
        visible = showNotes,
        enter = fadeIn(AppMotion.fastEffectsSpec()) + expandVertically(AppMotion.defaultSpatialSpec()),
        exit = fadeOut(AppMotion.fastEffectsSpec()) + shrinkVertically(AppMotion.defaultSpatialSpec())) {
          NotesField(
              state = description,
              label = stringResource(R.string.description),
              enabled = enabled,
              focusRequester = notesFocus)
        }
  }

  // Открыли кнопкой — сразу печатаем, без лишнего нажатия в поле.
  LaunchedEffect(placeOpen) { if (placeOpen) runCatching { placeFocus.requestFocus() } }
  LaunchedEffect(notesOpen) { if (notesOpen) runCatching { notesFocus.requestFocus() } }
}

/** Поле деталей в стиле формы: без рамки, на ступень темнее секции, описание растёт по тексту. */
@Composable
fun DetailField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    focusRequester: FocusRequester,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
) {
  TextField(
      value = value,
      onValueChange = onValueChange,
      label = { Text(label) },
      leadingIcon = { Icon(icon, contentDescription = null) },
      singleLine = singleLine,
      minLines = 1,
      maxLines = if (singleLine) 1 else 6,
      enabled = enabled,
      textStyle = typography.bodyLarge,
      keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
      shape = shapes.extraLarge,
      colors = detailFieldColors(),
      modifier = modifier.fillMaxWidth().focusRequester(focusRequester))
}

/**
 * Описание: тот же вид, что у [DetailField], но на [TextFieldState]. Описания из Zoom и Meet —
 * тысячи символов, и строковое поле пересобирало форму и весь текст на каждую букву.
 */
@Composable
private fun NotesField(
    state: TextFieldState,
    label: String,
    enabled: Boolean,
    focusRequester: FocusRequester,
) {
  TextField(
      state = state,
      label = { Text(label) },
      leadingIcon = { Icon(Icons.Rounded.Notes, contentDescription = null) },
      // Без своей прокрутки — растёт целиком, листается вся форма: иначе свайп по полю делили
      // две прокрутки, и шторка тянулась вдвое медленнее пальца.
      lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 1),
      enabled = enabled,
      textStyle = typography.bodyLarge,
      keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
      shape = shapes.extraLarge,
      colors = detailFieldColors(),
      modifier = Modifier.fillMaxWidth().focusRequester(focusRequester))
}

/** Без рамки, на ступень темнее секции. */
@Composable
private fun detailFieldColors() =
    TextFieldDefaults.colors(
        focusedContainerColor = colorScheme.surfaceContainerHighest,
        unfocusedContainerColor = colorScheme.surfaceContainerHigh,
        disabledContainerColor = colorScheme.surfaceContainerHigh,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent)

/** Короткая подпись напоминания для кнопки: «15 мин», «1 ч», «Накануне»; нет — «Напоминание». */
@Composable
private fun reminderShortLabel(minutes: Int?, allDay: Boolean): String =
    when {
      minutes == null -> stringResource(R.string.reminder)
      minutes == 0 -> stringResource(R.string.reminder_at_start_short)
      allDay && minutes < 0 -> stringResource(R.string.reminder_on_day_short)
      allDay && minutes <= MINUTES_IN_DAY -> stringResource(R.string.reminder_day_before_short)
      minutes % MINUTES_IN_DAY == 0 || allDay ->
          stringResource(R.string.reminder_days_short, (minutes + MINUTES_IN_DAY - 1) / MINUTES_IN_DAY)
      minutes % 60 == 0 -> stringResource(R.string.duration_hours_short, minutes / 60)
      else -> stringResource(R.string.duration_minutes_short, minutes)
    }

private const val MINUTES_IN_DAY = 24 * 60

