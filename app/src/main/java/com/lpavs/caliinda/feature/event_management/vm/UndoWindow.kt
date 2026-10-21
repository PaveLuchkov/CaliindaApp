package com.lpavs.caliinda.feature.event_management.vm

import android.content.Context
import android.view.accessibility.AccessibilityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сколько длится окно «Отменить» после удаления: 3,5 с, а если в настройках доступности просят
 * больше времени на действия — сколько попросили.
 */
@Singleton
class UndoWindow @Inject constructor(@ApplicationContext private val context: Context) {
  fun millis(): Long {
    val manager = context.getSystemService(AccessibilityManager::class.java) ?: return BASE_MS
    return manager
        .getRecommendedTimeoutMillis(
            BASE_MS.toInt(),
            AccessibilityManager.FLAG_CONTENT_TEXT or AccessibilityManager.FLAG_CONTENT_CONTROLS)
        .toLong()
  }

  private companion object {
    const val BASE_MS = 3_500L
  }
}
