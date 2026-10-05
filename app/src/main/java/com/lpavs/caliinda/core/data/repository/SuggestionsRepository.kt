package com.lpavs.caliinda.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lpavs.caliinda.core.data.suggestions.BuiltInChips
import com.lpavs.caliinda.core.data.suggestions.SuggestionChip
import com.lpavs.caliinda.core.data.suggestions.SuggestionChipsCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuggestionsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

  private object PreferencesKeys {
    /** Список чипов целиком (JSON). Нет ключа — встроенные по умолчанию. */
    val CHIPS = stringPreferencesKey("suggestion_chips_v1")
  }

  /** Чипы в порядке пользователя; без сохранённого списка — встроенные. */
  val chipsFlow: Flow<List<SuggestionChip>> =
      dataStore.data.map { prefs ->
        prefs[PreferencesKeys.CHIPS]?.let(SuggestionChipsCodec::decode) ?: BuiltInChips.defaults
      }

  suspend fun saveChips(chips: List<SuggestionChip>) {
    dataStore.edit { it[PreferencesKeys.CHIPS] = SuggestionChipsCodec.encode(chips) }
  }

  /** Вернуть встроенный набор: правки и свои чипы удаляются, история нажатий остаётся. */
  suspend fun resetChips() {
    dataStore.edit { it.remove(PreferencesKeys.CHIPS) }
  }

  suspend fun incrementWeight(suggestion: String) {
    val key = intPreferencesKey("${suggestion}_weight")
    dataStore.edit { preferences ->
      val currentWeight = preferences[key] ?: 0
      preferences[key] = currentWeight + 1
    }
  }

  suspend fun getWeights(): Map<String, Int> {
    return dataStore.data
        .map { preferences ->
          preferences
              .asMap()
              .keys
              .filter { it.name.endsWith("_weight") }
              .associate { key ->
                val chipKey = key.name.removeSuffix("_weight")
                val weight = preferences[key] as? Int ?: 0
                chipKey to weight
              }
        }
        .first()
  }
}
