package com.lpavs.caliinda.feature.event_management.ui.shared.sections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.core.data.location.AddressSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** Подсказки адресов под полем места — с задержкой, чтобы не дёргать геокодер на каждую букву. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocationSuggestionsViewModel @Inject constructor(private val search: AddressSearch) :
    ViewModel() {
  private val query = MutableStateFlow("")

  val suggestions: StateFlow<List<String>> =
      query
          .debounce(DEBOUNCE_MS)
          .distinctUntilChanged()
          .flatMapLatest { text ->
            flow {
              emit(
                  if (text.trim().length < MIN_LENGTH) emptyList()
                  else search.suggest(text.trim()).filterNot { it.equals(text.trim(), true) })
            }
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun onQueryChange(text: String) {
    query.value = text
  }

  private companion object {
    const val DEBOUNCE_MS = 400L
    const val MIN_LENGTH = 3
  }
}
