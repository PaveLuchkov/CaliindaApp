package com.lpavs.caliinda.feature.settings.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

  val timeZone: StateFlow<String> =
      settingsRepository.timeZoneFlow.stateIn(
          viewModelScope, SharingStarted.WhileSubscribed(5000), ZoneId.systemDefault().id)

  fun updateTimeZoneSetting(zoneId: String) {
    if (ZoneId.getAvailableZoneIds().contains(zoneId)) {
      viewModelScope.launch { settingsRepository.saveTimeZone(zoneId) }
    } else {
      Log.e(TAG, "Attempted to save invalid time zone ID: $zoneId")
    }
  }

  val botTemperState: StateFlow<String> =
      settingsRepository.botTemperFlow.stateIn(
          viewModelScope, SharingStarted.WhileSubscribed(5000), "")

  fun updateBotTemperSetting(newTemper: String) {
    viewModelScope.launch { settingsRepository.saveBotTemper(newTemper) }
  }

  companion object {
    private const val TAG = "SettingsViewModel"
  }
}
