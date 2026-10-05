package com.lpavs.caliinda.feature.settings.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.core.data.repository.SuggestionsRepository
import com.lpavs.caliinda.core.data.suggestions.SuggestionChip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Редактор списка чипов-подсказок: правка, добавление, удаление и сброс с отменой. */
@HiltViewModel
class ChipsSettingsViewModel
@Inject
constructor(private val repository: SuggestionsRepository) : ViewModel() {

  val chips: StateFlow<List<SuggestionChip>> =
      repository.chipsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  /** Сохраняет правку; новый чип (его id ещё нет в списке) встаёт в начало. */
  fun save(chip: SuggestionChip) = edit { list ->
    if (list.any { it.id == chip.id }) list.map { if (it.id == chip.id) chip else it }
    else listOf(chip) + list
  }

  fun newChipId(): String = "custom_" + UUID.randomUUID()

  /** Удаляет и возвращает прежний список — для «Отменить». */
  suspend fun delete(id: String): List<SuggestionChip> {
    val before = repository.chipsFlow.first()
    repository.saveChips(before.filterNot { it.id == id })
    return before
  }

  suspend fun reset(): List<SuggestionChip> {
    val before = repository.chipsFlow.first()
    repository.resetChips()
    return before
  }

  fun restore(list: List<SuggestionChip>) {
    viewModelScope.launch { repository.saveChips(list) }
  }

  private fun edit(transform: (List<SuggestionChip>) -> List<SuggestionChip>) {
    viewModelScope.launch { repository.saveChips(transform(repository.chipsFlow.first())) }
  }
}
