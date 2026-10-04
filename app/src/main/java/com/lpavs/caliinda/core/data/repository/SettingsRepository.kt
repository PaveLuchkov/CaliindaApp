package com.lpavs.caliinda.core.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lpavs.caliinda.feature.settings.vm.ThemeMode
import java.io.IOException
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {
  private object PreferencesKeys {
    val TIME_ZONE = stringPreferencesKey("time_zone")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val INTRO_COMPLETED = booleanPreferencesKey("introduction")
    val DEFAULT_CALENDAR_ID = longPreferencesKey("default_calendar_id")
  }
  val introFlow: Flow<Boolean> =
      dataStore.data
          .catch { e ->
            if (e is IOException) {
              Log.e(TAG, "Error reading preferences", e)
              emit(emptyPreferences())
            } else throw e
          }
          .map { prefs -> prefs[PreferencesKeys.INTRO_COMPLETED] ?: false }

  suspend fun isIntroFinished(): Boolean {
    return introFlow.first()
  }

  suspend fun saveIntroComplete() {
    dataStore.edit { prefs -> prefs[PreferencesKeys.INTRO_COMPLETED] = true }
  }

  val themeModeFlow: Flow<ThemeMode> =
      dataStore.data
          .catch { exception ->
            if (exception is IOException) {
              Log.e(TAG, "Error reading theme preferences.", exception)
              emit(emptyPreferences())
            } else {
              throw exception
            }
          }
          .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME_MODE]
            if (themeName != null) {
              try {
                ThemeMode.valueOf(themeName)
              } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Unknown theme mode found: $themeName. Fallback to SYSTEM.")
                ThemeMode.SYSTEM
              }
            } else {
              ThemeMode.SYSTEM
            }
          }

  suspend fun saveThemeMode(mode: ThemeMode) {
    try {
      dataStore.edit { prefs -> prefs[PreferencesKeys.THEME_MODE] = mode.name }
      Log.i(TAG, "Saved theme mode: ${mode.name}")
    } catch (e: IOException) {
      Log.e(TAG, "Error saving theme mode.", e)
    }
  }

  val timeZoneFlow: Flow<String> =
      dataStore.data
          .catch { exception ->
            if (exception is IOException) {
              Log.e(TAG, "Error reading preferences.", exception)
              emit(emptyPreferences())
            } else throw exception
          }
          .map { preferences ->
            preferences[PreferencesKeys.TIME_ZONE] ?: ZoneId.systemDefault().id
          }

  suspend fun saveTimeZone(timeZone: String) {
    try {
      dataStore.edit { preferences -> preferences[PreferencesKeys.TIME_ZONE] = timeZone }
      Log.i(TAG, "Saved time zone setting.")
    } catch (e: IOException) {
      Log.e(TAG, "Error saving time zone.", e)
    }
  }

  /** Календарь, в который создаются новые события. null — выбрать автоматически. */
  val defaultCalendarIdFlow: Flow<Long?> =
      dataStore.data
          .catch { e ->
            if (e is IOException) {
              Log.e(TAG, "Error reading preferences", e)
              emit(emptyPreferences())
            } else throw e
          }
          .map { prefs -> prefs[PreferencesKeys.DEFAULT_CALENDAR_ID] }

  suspend fun saveDefaultCalendarId(calendarId: Long) {
    try {
      dataStore.edit { prefs -> prefs[PreferencesKeys.DEFAULT_CALENDAR_ID] = calendarId }
    } catch (e: IOException) {
      Log.e(TAG, "Error saving default calendar.", e)
    }
  }

  companion object {
    private const val TAG = "SettingsRepository"
  }
}
