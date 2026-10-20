package com.lpavs.caliinda.feature.widget.stack

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.SizeF
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.lpavs.caliinda.R
import com.lpavs.caliinda.app.MainActivity
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.ui.util.displayTitle
import com.lpavs.caliinda.feature.widget.WidgetEntryPoint
import com.lpavs.caliinda.feature.widget.openEventIntent
import com.lpavs.caliinda.feature.widget.scheduleWidgetUpdate
import com.lpavs.caliinda.feature.widget.timeRange
import com.lpavs.caliinda.feature.widget.widgetShapeBitmap
import com.lpavs.caliinda.feature.widget.widgetTitle
import dagger.hilt.android.EntryPointAccessors
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Стопка событий дня: сверху то, что идёт сейчас, свайп вверх-вниз листает дальше. В Glance нет
 * StackView, поэтому виджет на обычных RemoteViews; картинки названий и фигуры — общие с
 * остальными виджетами.
 */
class StackWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
    refresh(context, ids)
  }

  // Размер поменялся — картинки нужно перерисовать под него.
  override fun onAppWidgetOptionsChanged(
      context: Context,
      manager: AppWidgetManager,
      id: Int,
      options: Bundle,
  ) {
    refresh(context, intArrayOf(id))
  }

  private fun refresh(context: Context, ids: IntArray) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
      try {
        update(context, ids)
      } finally {
        pending.finish()
      }
    }
  }

  companion object {
    /** Для [com.lpavs.caliinda.feature.widget.WidgetRefresher]: календарь поменялся. */
    suspend fun updateAll(context: Context) {
      val ids =
          AppWidgetManager.getInstance(context)
              .getAppWidgetIds(ComponentName(context, StackWidgetProvider::class.java))
      if (ids.isNotEmpty()) update(context, ids)
    }

    private suspend fun update(context: Context, ids: IntArray) {
      val entryPoint =
          EntryPointAccessors.fromApplication(
              context.applicationContext, WidgetEntryPoint::class.java)
      val data = entryPoint.loadStack()
      scheduleWidgetUpdate(context, data.nextChange, StackWidgetProvider::class.java)
      val manager = AppWidgetManager.getInstance(context)
      ids.forEach { id ->
        // Под каждый размер виджета (портрет и ландшафт) — свои картинки.
        val sizes =
            manager
                .getAppWidgetOptions(id)
                .getParcelableArrayList(
                    AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
                ?.takeIf { it.isNotEmpty() } ?: listOf(SizeF(250f, 150f))
        val views = RemoteViews(sizes.associateWith { stackViews(context, data, it) })
        manager.updateAppWidget(id, views)
      }
    }
  }
}

internal class StackData(
    val hasAccess: Boolean,
    val events: List<EventDto>,
    val now: Instant,
    val zone: ZoneId,
    val nextChange: Instant,
)

private suspend fun WidgetEntryPoint.loadStack(now: Instant = Instant.now()): StackData {
  permissionManager().refresh()
  val zone = settingsRepository().zoneFlow.first()
  val today = now.atZone(zone).toLocalDate()
  val midnight = today.plusDays(1).atStartOfDay(zone).toInstant()
  if (!permissionManager().isGranted.value) {
    return StackData(false, emptyList(), now, zone, midnight)
  }
  val events =
      calendarRepository()
          .getDayFlow(today)
          .first()
          .events
          .filter { !it.isAllDay && it.endTime.isAfter(now) }
          .sortedBy { it.startTime }
  val nextChange =
      events
          .flatMap { listOf(it.startTime, it.endTime) }
          .filter { it.isAfter(now) }
          .minOrNull()
          ?.coerceAtMost(midnight) ?: midnight
  return StackData(true, events, now, zone, nextChange)
}

/** Цвета карточки днём и ночью: RemoteViews сам выберет нужные при смене темы. */
private class CardColors(val container: ColorStateList, val content: ColorStateList)

private fun colors(context: Context, pick: (ColorScheme) -> Pair<Color, Color>): Pair<CardColors, CardColors> {
  fun of(scheme: ColorScheme) =
      pick(scheme).let { (container, content) ->
        CardColors(
            ColorStateList.valueOf(container.toArgb()), ColorStateList.valueOf(content.toArgb()))
      }
  return of(dynamicLightColorScheme(context)) to of(dynamicDarkColorScheme(context))
}

private fun stackViews(context: Context, data: StackData, size: SizeF): RemoteViews {
  val views = RemoteViews(context.packageName, R.layout.widget_stack)
  val (emptyDay, emptyNight) =
      colors(context) { it.surfaceVariant to it.onSurfaceVariant }
  views.setColorStateList(
      R.id.stack_empty, "setBackgroundTintList", emptyDay.container, emptyNight.container)
  views.setColorStateList(R.id.stack_empty, "setTextColor", emptyDay.content, emptyNight.content)
  views.setTextViewText(
      R.id.stack_empty,
      context.getString(
          if (data.hasAccess) R.string.widget_no_more_events else R.string.widget_no_access))
  views.setEmptyView(R.id.stack, R.id.stack_empty)

  val items = RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(1)
  data.events.forEachIndexed { index, event ->
    items.addItem(event.id.hashCode().toLong(), cardViews(context, data, event, index, size))
  }
  views.setRemoteAdapter(R.id.stack, items.build())

  val open =
      PendingIntent.getActivity(
          context,
          0,
          // Флаги — в шаблоне: как у «+», открытое приложение получит интент в onNewIntent.
          Intent(context, MainActivity::class.java)
              .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
          // MUTABLE: в шаблон подставляется fill-in интент карточки.
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
  views.setPendingIntentTemplate(R.id.stack, open)
  return views
}

private fun cardViews(
    context: Context,
    data: StackData,
    event: EventDto,
    index: Int,
    size: SizeF,
): RemoteViews {
  val isNow = !event.startTime.isAfter(data.now)
  val (day, night) =
      colors(context) {
        if (isNow) it.tertiaryContainer to it.onTertiaryContainer
        else it.primaryContainer to it.onPrimaryContainer
      }
  // StackView рисует карточку меньше виджета: ~80% ширины и высоты, остальное — «хвост» стопки.
  val cardW = size.width * 0.8f
  val cardH = size.height * 0.8f
  val title =
      widgetTitle(
          context = context,
          text = event.displayTitle(context),
          maxWidthDp = cardW - 32f,
          maxHeightDp = cardH - 24f - 40f,
          textSizeSp = 24f,
          minTextSizeSp = 14f,
          maxLines = 3)

  return RemoteViews(context.packageName, R.layout.widget_stack_item).apply {
    setViewLayoutWidth(R.id.card, cardW, TypedValue.COMPLEX_UNIT_DIP)
    setViewLayoutHeight(R.id.card, cardH, TypedValue.COMPLEX_UNIT_DIP)
    setColorStateList(R.id.card, "setBackgroundTintList", day.container, night.container)
    setImageViewBitmap(R.id.card_shape, widgetShapeBitmap(event.id, cardW / cardH))
    setColorStateList(R.id.card_shape, "setImageTintList", day.content, night.content)
    setImageViewBitmap(R.id.card_title, title.glyphs)
    setColorStateList(R.id.card_title, "setImageTintList", day.content, night.content)
    setContentDescription(R.id.card_title, event.displayTitle(context))
    title.emoji?.let { setImageViewBitmap(R.id.card_title_emoji, it) }
    setTextViewText(
        R.id.card_label,
        context.getString(
            R.string.stack_label,
            context.getString(if (isNow) R.string.stack_now else R.string.stack_next),
            index + 1,
            data.events.size))
    setColorStateList(R.id.card_label, "setTextColor", day.content, night.content)
    setTextViewText(R.id.card_time, event.timeRange(context, data.zone))
    setColorStateList(R.id.card_time, "setTextColor", day.content, night.content)
    // Карточка открывает своё событие: действие и данные подставятся в шаблон.
    setOnClickFillInIntent(
        R.id.card, openEventIntent(context, event, data.zone, data.now.atZone(data.zone).toLocalDate()))
  }
}
