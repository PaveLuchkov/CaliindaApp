package com.example.caliinda.data.repo
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val TAG = "SettingsRepository"

    private object PreferencesKeys {
        val BOT_TEMPER = stringPreferencesKey("bot_temper")
        val TIME_ZONE = stringPreferencesKey("time_zone")
        val USE_12_HOUR_FORMAT = booleanPreferencesKey("use_12_hour_format")
    }

    val botTemperFlow: Flow<String> = dataStore.data
        .catch { exception ->
            // Обработка ошибок чтения DataStore
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences.", exception)
                emit(emptyPreferences()) // Возвращаем пустые преференсы при ошибке IO
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.BOT_TEMPER] ?: ""
        }

    suspend fun saveBotTemper(temper: String) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.BOT_TEMPER] = temper
            }
            Log.i(TAG, "Saved bot temper setting.")
        } catch (exception: IOException) {
            Log.e(TAG, "Error writing preferences.", exception)
        } catch (exception: Exception) {
            Log.e(TAG, "Unexpected error writing preferences.", exception)
        }
    }

    val timeZoneFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences.", exception)
                emit(emptyPreferences())
            } else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.TIME_ZONE] ?: ""
        }

    suspend fun saveTimeZone(timeZone: String) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.TIME_ZONE] = timeZone
            }
            Log.i(TAG, "Saved time zone setting.")
        } catch (e: IOException) {
            Log.e(TAG, "Error saving time zone.", e)
        }
    }

}