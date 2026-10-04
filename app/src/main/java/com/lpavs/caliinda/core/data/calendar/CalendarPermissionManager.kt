package com.lpavs.caliinda.core.data.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarPermissionManager
@Inject
constructor(@ApplicationContext private val context: Context) {
  private val _isGranted = MutableStateFlow(check())
  val isGranted: StateFlow<Boolean> = _isGranted.asStateFlow()

  /** Перепроверяет разрешения (после ответа на запрос или возврата из настроек). */
  fun refresh() {
    _isGranted.value = check()
  }

  private fun check(): Boolean =
      PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
      }

  companion object {
    val PERMISSIONS =
        arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
  }
}
