package com.lpavs.caliinda.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuggestionsRepository @Inject constructor(private val dataStore: DataStore<Preferences>)

{
  private object PreferencesKeys {
    val SUGGESTION_WEIGHTS = stringPreferencesKey("suggestion_weights")
  }

  suspend fun incrementWeight(suggestion: String) {
    val key = intPreferencesKey("${suggestion}_weight")
    dataStore.edit { preferences ->
      val currentWeight = preferences[key] ?: 0
      preferences[key] = currentWeight + 1
    }
  }

  suspend fun getWeights(): Map<String, Int> {
    return dataStore.data.map { preferences ->
      preferences.asMap().keys
        .filter { it.name.endsWith("_weight") }
        .associate { key ->
          val chipKey = key.name.removeSuffix("_weight")
          val weight = preferences[key] as? Int ?: 0
          chipKey to weight
        }
    }.first()
  }
}
