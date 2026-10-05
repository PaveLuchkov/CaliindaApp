package com.lpavs.caliinda.feature.event_management.ui.shared.sections.suggestions

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.core.data.repository.SuggestionsRepository
import com.lpavs.caliinda.core.data.suggestions.orderChips
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.SugNameChips
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class SuggestionsViewModel
@Inject
constructor(
    private val suggestionsRepository: SuggestionsRepository,
    private val application: Application
) : ViewModel() {
  private val timeContext = MutableStateFlow(LocalTime.now())
  private val clicks = MutableStateFlow<Map<String, Int>>(emptyMap())

  init {
    viewModelScope.launch { clicks.value = suggestionsRepository.getWeights() }
  }

  /** Чипы для времени начала: попавшие в свои часы — первыми (см. [orderChips]). */
  val suggestionChips: StateFlow<List<SugNameChips>> =
      combine(suggestionsRepository.chipsFlow, timeContext, clicks) { chips, time, weights ->
            orderChips(chips, weights, time).map { it.toUi(application) }
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun updateSortContext(startTime: LocalTime?, isAllDay: Boolean) {
    timeContext.value = if (isAllDay || startTime == null) LocalTime.now() else startTime
  }

  fun onChipClicked(chip: SugNameChips) {
    viewModelScope.launch {
      suggestionsRepository.incrementWeight(chip.key)
      clicks.value = suggestionsRepository.getWeights()
    }
  }
}
