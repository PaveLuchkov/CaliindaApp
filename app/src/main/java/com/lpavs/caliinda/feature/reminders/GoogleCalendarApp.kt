package com.lpavs.caliinda.feature.reminders

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Есть ли Google Календарь: он сам показывает напоминания, и наши были бы дублями. */
@Singleton
class GoogleCalendarApp @Inject constructor(@ApplicationContext private val context: Context) {
  fun isActive(): Boolean =
      // Не установлен — NameNotFoundException; отключён пользователем — enabled = false.
      runCatching { context.packageManager.getApplicationInfo(PACKAGE, 0).enabled }.getOrDefault(false)

  companion object {
    const val PACKAGE = "com.google.android.calendar"
  }
}
