package com.lpavs.caliinda.feature.widget

import android.content.Context
import android.content.Intent
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import com.lpavs.caliinda.app.LaunchRequests
import com.lpavs.caliinda.app.MainActivity
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import java.time.LocalDate
import java.time.ZoneId

/**
 * Интент «открыть событие»: приложение покажет его день и подробности. Уже идущее с прошлых
 * дней (проект, ночная смена) — открываем сегодня, а не день начала недели назад.
 * CLEAR_TOP + singleTop — как у кнопки «+»: открытое приложение получит его в onNewIntent.
 */
fun openEventIntent(context: Context, event: EventDto, zone: ZoneId, today: LocalDate): Intent {
  val start = event.startTime.atZone(zone).toLocalDate()
  val day = if (start.isBefore(today)) today else start
  return Intent(context, MainActivity::class.java)
      .setAction(LaunchRequests.ACTION_OPEN_EVENT)
      .putExtra(LaunchRequests.EXTRA_EVENT_ID, event.eventId)
      .putExtra(LaunchRequests.EXTRA_INSTANCE_BEGIN, event.instanceBegin)
      .putExtra(LaunchRequests.EXTRA_EPOCH_DAY, day.toEpochDay())
      .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
}

fun openEventAction(context: Context, event: EventDto, zone: ZoneId, today: LocalDate): Action =
    actionStartActivity(openEventIntent(context, event, zone, today))
