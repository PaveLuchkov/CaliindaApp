package com.lpavs.caliinda.feature.event_management.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.SugNameChips
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalTextApi::class)
@Composable
fun CustomOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null
) {
  TextField(
      value = value,
      onValueChange = onValueChange,
      label = { Text(text = label, textAlign = TextAlign.Start) },
      modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
      colors =
          OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Color.Transparent,
              unfocusedBorderColor = Color.Transparent,
              focusedContainerColor = Color.Transparent,
              unfocusedContainerColor = Color.Transparent,
          ),
      keyboardOptions = keyboardOptions,
      textStyle = typography.headlineMedium.copy(textAlign = TextAlign.Start, fontFamily = FontFamily(
          Font(
              R.font.robotoflex_variable,
              variationSettings =
                  FontVariation.Settings(
                      FontVariation.weight(750),
                  )))),
      enabled = enabled,
      singleLine = true,
      isError = isError,
      supportingText = supportingText)
}

@Composable
fun ChipsRow(
    chips: List<SugNameChips>,
    onChipClick: (SugNameChips) -> Unit,
    enabled: Boolean,
    lazyListState: LazyListState
) {
  LazyRow(
      state = lazyListState,
      modifier = Modifier.fillMaxWidth().padding(horizontal = cuid.padding),
      horizontalArrangement = Arrangement.spacedBy(cuid.padding),
      contentPadding = PaddingValues(bottom = cuid.padding)) {
        items(chips, key = { it.name }) { chip ->
          SuggestionChip(
              onClick = { onChipClick(chip) },
              label = { Text(chip.name) },
              modifier = Modifier.height(35.dp),
              enabled = enabled)
        }
      }
}

@Composable
internal fun DatePickerField(
    date: LocalDate?,
    dateFormatter: DateTimeFormatter,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  ModernClickableTextField(
      value = date?.format(dateFormatter) ?: "",
      isLoading = isLoading,
      onClick = onClick,
      modifier = modifier)
}

@Composable
internal fun TimePickerField(
    time: LocalTime?,
    timeFormatter: DateTimeFormatter,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
  ModernClickableTextField(
      value = time?.format(timeFormatter) ?: "--:--",
      isLoading = isLoading,
      onClick = onClick,
      onLongClick = onLongClick,
      modifier = modifier)
}

@Composable
private fun ModernClickableTextField(
    value: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
  val haptics = LocalHapticFeedback.current
  Box(
      modifier =
          modifier
              .height(45.dp)
              .clip(shape = RoundedCornerShape(cuid.ContainerCornerRadius))
              .background(colorScheme.secondaryContainer)) {
        Row(
            modifier =
                Modifier.fillMaxSize()
                    .combinedClickable(
                        enabled = !isLoading,
                        onClick = onClick,
                        onLongClick = {
                          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                          onLongClick?.invoke()
                        },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
              Text(text = value, color = colorScheme.onSecondaryContainer)
            }
      }
}
