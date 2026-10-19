package com.lpavs.caliinda.feature.event_management.ui.shared.sections

import com.lpavs.caliinda.core.data.suggestions.ChipTerms
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.core.data.suggestions.insertEmoji
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.lpavs.caliinda.R
import com.lpavs.caliinda.feature.event_management.ui.shared.ChipsRow
import com.lpavs.caliinda.feature.event_management.ui.shared.CustomOutlinedTextField
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.suggestions.SuggestionsViewModel

data class SugNameChips(
    val key: String,
    val name: String,
    val fullText: String,
    val terms: ChipTerms = ChipTerms(listOf(name, fullText), emptyList()),
    /** Уместен прямо сейчас (день и часы) — чуть выделен в ряду. */
    val highlighted: Boolean = false,
)

@Composable
fun EventNameSection(
    summary: String,
    summaryError: String?,
    onSummaryChange: (String) -> Unit,
    onSummaryErrorChange: (String?) -> Unit,
    suggestionsViewModel: SuggestionsViewModel = hiltViewModel(),
    isLoading: Boolean,
    suggestedChips: List<SugNameChips>,
    onImeDone: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
  val emojiSuggestions by suggestionsViewModel.emojiSuggestions.collectAsStateWithLifecycle()
  // Курсор живёт здесь; текст — у формы. Пришёл другой текст снаружи (чип) — курсор в конец.
  var field by remember { mutableStateOf(TextFieldValue(summary, TextRange(summary.length))) }
  if (field.text != summary) field = TextFieldValue(summary, TextRange(summary.length))
  CustomOutlinedTextField(
      value = field,
      onValueChange = {
        field = it
        if (it.text != summary) {
          onSummaryChange(it.text)
          onSummaryErrorChange(null)
        }
      },
      label = stringResource(R.string.event_name),
      modifier =
          Modifier.fillMaxWidth()
              .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
      // «Готово» на клавиатуре сохраняет: для быстрого события хватает названия.
      keyboardOptions =
          KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = { if (summary.isNotBlank()) onImeDone() }),
      isError = summaryError != null,
      supportingText = { if (summaryError != null) Text(summaryError) },
      enabled = !isLoading,
      leadingIcon = {
        EmojiButton(
            suggestions = emojiSuggestions,
            onPick = { emoji ->
              val (text, cursor) =
                  insertEmoji(field.text, field.selection.start, field.selection.end, emoji)
              field = TextFieldValue(text, TextRange(cursor))
              onSummaryChange(text)
              onSummaryErrorChange(null)
            },
            enabled = !isLoading)
      },
  )
  // Набранное название поднимает подходящие чипы вперёд.
  LaunchedEffect(summary) { suggestionsViewModel.updateQuery(summary) }
  val lazyListState = rememberLazyListState()
  LaunchedEffect(suggestedChips) { lazyListState.animateScrollToItem(index = 0) }
  ChipsRow(
      chips = suggestedChips,
      onChipClick = { chip ->
        onSummaryChange(chip.fullText)
        suggestionsViewModel.onChipClicked(chip)
      },
      enabled = !isLoading,
      lazyListState = lazyListState)
}
