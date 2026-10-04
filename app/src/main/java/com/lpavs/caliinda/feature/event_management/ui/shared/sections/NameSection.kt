package com.lpavs.caliinda.feature.event_management.ui.shared.sections

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.lpavs.caliinda.R
import com.lpavs.caliinda.feature.event_management.ui.shared.ChipsRow
import com.lpavs.caliinda.feature.event_management.ui.shared.CustomOutlinedTextField
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.suggestions.SuggestionsViewModel

data class SugNameChips(val key: String, val name: String, val fullText: String)

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
  CustomOutlinedTextField(
      value = summary,
      onValueChange = {
        onSummaryChange(it)
        onSummaryErrorChange(null)
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
  )
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
