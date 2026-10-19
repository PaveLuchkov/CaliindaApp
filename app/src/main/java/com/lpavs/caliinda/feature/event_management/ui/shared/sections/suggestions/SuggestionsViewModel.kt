package com.lpavs.caliinda.feature.event_management.ui.shared.sections.suggestions

import com.lpavs.caliinda.core.data.suggestions.rankByQuery
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.core.data.repository.CalendarRepository
import com.lpavs.caliinda.core.data.repository.SuggestionsRepository
import com.lpavs.caliinda.core.data.suggestions.EmojiIndex
import com.lpavs.caliinda.core.data.suggestions.EmojiSuggestions
import com.lpavs.caliinda.core.data.suggestions.ChipFit
import com.lpavs.caliinda.core.data.suggestions.fit
import com.lpavs.caliinda.core.data.suggestions.orderChips
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.SugNameChips
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class SuggestionsViewModel
@Inject
constructor(
    private val suggestionsRepository: SuggestionsRepository,
    private val calendarRepository: CalendarRepository,
    private val application: Application
) : ViewModel() {
  /** День и время начала события — от них зависит, какие чипы уместны. */
  private val timeContext = MutableStateFlow(LocalDateTime.now())
  private val clicks = MutableStateFlow<Map<String, Int>>(emptyMap())
  private val query = MutableStateFlow("")

  /** Эмодзи из истории календаря — читаем один раз на открытие формы. */
  private val emojiIndex = MutableStateFlow(EmojiIndex(emptyList()))

  init {
    viewModelScope.launch { clicks.value = suggestionsRepository.getWeights() }
    viewModelScope.launch { emojiIndex.value = EmojiIndex(calendarRepository.recentTitles()) }
  }

  /** Панель эмодзи: подходящие к набранному названию и все свои. */
  val emojiSuggestions: StateFlow<EmojiSuggestions> =
      combine(emojiIndex, query) { index, text -> index.suggest(text) }
          .stateIn(
              viewModelScope,
              SharingStarted.WhileSubscribed(5000),
              EmojiIndex(emptyList()).suggest(""))

  /**
   * Чипы для дня и времени начала: уместные — первыми (см. [orderChips]), первые из них
   * подсвечены; подходящие к набранному названию — ещё раньше (см. [rankByQuery]).
   */
  val suggestionChips: StateFlow<List<SugNameChips>> =
      combine(suggestionsRepository.chipsFlow, timeContext, clicks, query) {
              chips,
              start,
              weights,
              text ->
            val day = start.dayOfWeek
            val time = start.toLocalTime()
            val ordered = orderChips(chips, weights, day, time)
            // Подсвечиваем немного: если выделено всё, не выделено ничего.
            val highlighted =
                ordered.filter { it.fit(day, time) == ChipFit.MATCH }.take(MAX_HIGHLIGHTED)
            val ui = ordered.map { it.toUi(application, highlighted = it in highlighted) }
            rankByQuery(ui, text) { it.terms }
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun updateSortContext(startDate: LocalDate, startTime: LocalTime?, isAllDay: Boolean) {
    val time = if (isAllDay || startTime == null) LocalTime.now() else startTime
    timeContext.value = startDate.atTime(time)
  }

  fun updateQuery(text: String) {
    query.value = text
  }

  fun onChipClicked(chip: SugNameChips) {
    viewModelScope.launch {
      suggestionsRepository.incrementWeight(chip.key)
      clicks.value = suggestionsRepository.getWeights()
    }
  }

  private companion object {
    const val MAX_HIGHLIGHTED = 3
  }
}
