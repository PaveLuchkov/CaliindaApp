package com.lpavs.caliinda.feature.event_management.ui.shared.sections

/** Напоминание в форме. */
sealed interface ReminderChoice {
  /** Как в настройках — своё значение для событий со временем и на весь день. */
  data object Default : ReminderChoice

  /** Выбрано вручную: за сколько минут, null — без напоминания. */
  data class Custom(val minutes: Int?) : ReminderChoice
}

/** Напоминания по умолчанию из настроек. */
data class ReminderDefaults(val timed: Int?, val allDay: Int?) {
  fun forEvent(isAllDay: Boolean): Int? = if (isAllDay) allDay else timed
}

/** Сколько минут до начала получится для события — с учётом настроек. */
fun ReminderChoice.minutes(defaults: ReminderDefaults, isAllDay: Boolean): Int? =
    when (this) {
      ReminderChoice.Default -> defaults.forEvent(isAllDay)
      is ReminderChoice.Custom -> minutes
    }
