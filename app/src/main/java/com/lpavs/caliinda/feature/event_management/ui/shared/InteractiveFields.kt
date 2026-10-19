package com.lpavs.caliinda.feature.event_management.ui.shared

import androidx.compose.runtime.getValue
import androidx.compose.foundation.text.KeyboardActions
import com.lpavs.caliinda.core.ui.theme.CaliindaFonts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SuggestionChip
import androidx.compose.animation.core.animateDpAsState
import com.lpavs.caliinda.core.ui.theme.AppMotion
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.core.ui.theme.CaliindaTheme
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.SugNameChips

@OptIn(ExperimentalTextApi::class)
@Composable
fun CustomOutlinedTextField(
    // TextFieldValue, а не строка: нужен курсор — эмодзи вставляется на его место.
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
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
              focusedContainerColor = colorScheme.surfaceContainerHighest,
              unfocusedContainerColor = colorScheme.surfaceContainerHigh,
          ),
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
      textStyle =
          typography.headlineMedium.copy(
              textAlign = TextAlign.Start,
              fontFamily =
                  CaliindaFonts.Heavy),
      enabled = enabled,
      singleLine = true,
      isError = isError,
      supportingText = supportingText,
      leadingIcon = leadingIcon,
      shape = RoundedCornerShape(cuid.ContainerCornerRadius))
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        items(chips, key = { it.key }) { chip ->
          // Уместный сейчас — таблетка: форма, а не заливка (заливка читается как «выбрано»).
          val corner by
              animateDpAsState(
                  if (chip.highlighted) CHIP_HEIGHT / 2 else CHIP_CORNER,
                  AppMotion.defaultSpatialSpec(),
                  label = "chipCorner")
          SuggestionChip(
              onClick = { onChipClick(chip) },
              label = { Text(chip.name) },
              modifier = Modifier.height(CHIP_HEIGHT).animateItem(),
              enabled = enabled,
              shape = RoundedCornerShape(corner))
        }
      }
}

private val CHIP_HEIGHT = 35.dp
private val CHIP_CORNER = 8.dp

@Preview(showBackground = true, wallpaper = Wallpapers.YELLOW_DOMINATED_EXAMPLE)
@Composable
fun CustomOutlinedTextFieldPreview() {
    CaliindaTheme {
        CustomOutlinedTextField(
            value = TextFieldValue("Name"),
            onValueChange = {},
            label = ""
        )
    }
}

