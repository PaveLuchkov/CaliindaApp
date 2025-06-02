package com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.ui.screens.main.components.UIDefaults.cuid
import com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.sections.SugNameChips
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, textAlign = TextAlign.Center) },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(cuid.ContainerCornerRadius),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colorScheme.surfaceContainerLow,
            unfocusedContainerColor = colorScheme.surfaceContainerLow,
        ),
        keyboardOptions = keyboardOptions,
        textStyle = typography.headlineMedium.copy(textAlign = TextAlign.Center),
        enabled = enabled,
        singleLine = true,
        isError = isError,
        supportingText = supportingText
    )
}

@Composable
fun ChipsRow(
    chips: List<SugNameChips>,
    onChipClick: (String) -> Unit,
    enabled: Boolean
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp)
    ) {
        items(chips, key = { it.name }) { chip ->
            SuggestionChip(
                onClick = { onChipClick(chip.fullText) },
                label = { Text(chip.name) },
                modifier = Modifier.height(35.dp),
                enabled = enabled
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatePickerField(
    label: String,
    date: LocalDate?,
    dateFormatter: DateTimeFormatter,
    isError: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    ClickableTextField(
        value = date?.format(dateFormatter) ?: "",
        label = label,
        isError = isError,
        isLoading = isLoading,
        onClick = onClick,
        modifier = modifier,
        textAlign = textAlign
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerField(
    label: String,
    time: LocalTime?,
    timeFormatter: DateTimeFormatter,
    isError: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ClickableTextField(
        value = time?.format(timeFormatter) ?: "--:--",
        label = label,
        isError = isError,
        isLoading = isLoading,
        onClick = onClick,
        modifier = modifier
    )
}

// Общий Composable для кликабельного текстового поля (паттерн с оверлеем)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClickableTextField(
    value: String,
    label: String,
    isError: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {}, // Не изменяется напрямую
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            enabled = !isLoading,
            shape = RoundedCornerShape(cuid.ContainerCornerRadius)
        )
        // Прозрачный Оверлей для клика
        Box(
            modifier = Modifier
                .matchParentSize() // Занимает все место родителя
                .clickable(
                    enabled = !isLoading,
                    onClick = onClick,
                    indication = null, // Можно убрать стандартную рябь
                    interactionSource = remember { MutableInteractionSource() } // Для обработки состояний нажатия оверлея, если нужно
                )
        )
    }
}
