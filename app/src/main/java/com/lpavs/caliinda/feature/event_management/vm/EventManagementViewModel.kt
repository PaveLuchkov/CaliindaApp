package com.lpavs.caliinda.feature.event_management.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class EventManagementViewModel @Inject constructor(
    settingsRepository: SettingsRepository
): ViewModel() {

    val timeZone: StateFlow<String> =
        settingsRepository.timeZoneFlow
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), ZoneId.systemDefault().id
        )

}
