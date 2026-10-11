package com.lpavs.caliinda.feature.settings.vm

import kotlinx.coroutines.flow.map
import com.lpavs.caliinda.feature.reminders.GoogleCalendarApp
import com.lpavs.caliinda.core.data.model.ThemeMode
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.core.data.calendar.model.DeviceCalendar
import com.lpavs.caliinda.core.data.repository.CalendarRepository
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import com.lpavs.caliinda.feature.live.LiveEventNotifier
import com.lpavs.caliinda.feature.widget.WidgetRefresher
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
    private val calendarRepository: CalendarRepository,
    private val liveEventNotifier: LiveEventNotifier,
    private val googleCalendar: GoogleCalendarApp,
    private val widgetRefresher: WidgetRefresher,
) : ViewModel() {

  val liveNotification: StateFlow<Boolean> =
      settingsRepository.liveNotificationFlow.stateIn(
          viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  val defaultReminder: StateFlow<Int?> =
      settingsRepository.defaultReminderFlow.stateIn(
          viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_REMINDER)

  val defaultAllDayReminder: StateFlow<Int?> =
      settingsRepository.defaultAllDayReminderFlow.stateIn(
          viewModelScope,
          SharingStarted.WhileSubscribed(5000),
          SettingsRepository.DEFAULT_ALL_DAY_REMINDER)

  fun setDefaultReminder(minutes: Int?) {
    viewModelScope.launch { settingsRepository.saveDefaultReminder(minutes) }
  }

  fun setDefaultAllDayReminder(minutes: Int?) {
    viewModelScope.launch { settingsRepository.saveDefaultAllDayReminder(minutes) }
  }

  /** Google Календарь сам показывает напоминания — наши по умолчанию выключены. */
  val googleCalendarActive: Boolean = googleCalendar.isActive()

  /** Свои напоминания: явная настройка или по умолчанию «да, если нет Google Календаря». */
  val reminderNotifications: StateFlow<Boolean> =
      settingsRepository.reminderNotificationsFlow
          .map { it ?: !googleCalendarActive }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), !googleCalendarActive)

  fun setReminderNotifications(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.saveReminderNotifications(enabled) }
  }

  fun setLiveNotification(enabled: Boolean) {
    viewModelScope.launch {
      settingsRepository.saveLiveNotification(enabled)
      liveEventNotifier.refresh()
    }
  }


  val timeZone: StateFlow<String> =
      settingsRepository.timeZoneFlow.stateIn(
          viewModelScope, SharingStarted.WhileSubscribed(5000), ZoneId.systemDefault().id)

  val themeMode: StateFlow<ThemeMode> =
      settingsRepository.themeModeFlow.stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000),
          initialValue = ThemeMode.SYSTEM)

  // 2. Обновление темы
  fun updateThemeMode(newMode: ThemeMode) {
    viewModelScope.launch {
      // Repository принимает ThemeMode, а не String, поэтому передаем объект целиком
      settingsRepository.saveThemeMode(newMode)
    }
  }

  fun updateTimeZoneSetting(zoneId: String) {
    if (ZoneId.getAvailableZoneIds().contains(zoneId)) {
      viewModelScope.launch { settingsRepository.saveTimeZone(zoneId) }
    } else {
      Log.e(TAG, "Attempted to save invalid time zone ID: $zoneId")
    }
  }

  val calendars: StateFlow<List<DeviceCalendar>> =
      calendarRepository
          .getWritableCalendars()
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val defaultCalendarId: StateFlow<Long?> =
      calendarRepository
          .getDefaultCalendarId()
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  fun selectDefaultCalendar(calendarId: Long) {
    viewModelScope.launch { settingsRepository.saveDefaultCalendarId(calendarId) }
  }

  /** Все видимые календари — и те, куда писать нельзя (праздники, дни рождения). */
  val visibleCalendars: StateFlow<List<DeviceCalendar>> =
      calendarRepository
          .getVisibleCalendars()
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val hiddenCalendarIds: StateFlow<Set<Long>> =
      settingsRepository.hiddenCalendarIdsFlow.stateIn(
          viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

  fun setCalendarShown(calendarId: Long, shown: Boolean) {
    viewModelScope.launch {
      settingsRepository.setCalendarHidden(calendarId, hidden = !shown)
      // Виджеты и live-уведомление не подписаны на настройки — будим их сами.
      widgetRefresher.refresh()
      liveEventNotifier.refresh()
    }
  }

  companion object {
    private const val TAG = "SettingsViewModel"
  }
}
