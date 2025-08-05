package com.lpavs.caliinda.feature.event_management.ui.shared.sections.suggestions

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.core.data.repository.SuggestionsRepository
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.SugNameChips
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
  private val _suggestionChips = MutableStateFlow<List<SugNameChips>>(emptyList())
  val suggestionChips: StateFlow<List<SugNameChips>> = _suggestionChips.asStateFlow()

  private val _timeContext = MutableStateFlow(LocalTime.now())

  init {
    viewModelScope.launch { _timeContext.collect { time -> loadAndSortSuggestions(time) } }
  }

  fun updateSortContext(startTime: LocalTime?, isAllDay: Boolean) {
    if (!isAllDay) {
      _timeContext.value = startTime
    } else {
      _timeContext.value = LocalTime.now()
    }
  }

  fun onChipClicked(chip: SugNameChips) {
    viewModelScope.launch {
      suggestionsRepository.incrementWeight(chip.key)
      loadAndSortSuggestions(_timeContext.value)
    }
  }

  private fun loadAndSortSuggestions(currentTime: LocalTime) {
    viewModelScope.launch {
      val weights = suggestionsRepository.getWeights()
      val baseChips = getSuggestedEventNames(application.applicationContext)
      val sortedChips =
          baseChips.sortedByDescending { chip ->
            val clickWeight = weights[chip.key] ?: 0
            val timeBonus = getTimeBasedBonus(chip.key, currentTime)
            clickWeight + timeBonus
          }
      _suggestionChips.value = sortedChips
    }
  }
}
