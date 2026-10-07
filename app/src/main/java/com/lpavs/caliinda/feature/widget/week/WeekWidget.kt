package com.lpavs.caliinda.feature.widget.week

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.app.MainActivity
import com.lpavs.caliinda.feature.widget.TitleImage
import com.lpavs.caliinda.feature.widget.WidgetEntryPoint
import com.lpavs.caliinda.feature.widget.date.dayNumberBitmap
import com.lpavs.caliinda.feature.widget.date.dayShapeBitmap
import com.lpavs.caliinda.feature.widget.scheduleWidgetUpdate
import com.lpavs.caliinda.feature.widget.widgetTitle
import dagger.hilt.android.EntryPointAccessors
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.flow.first

/**
 * Неделя одним взглядом: семь дней, у каждого точки занятости ([busyDots]) и полоска, если идёт
 * проект.
 * Сегодня — в фигуре дня, как на виджете даты.
 */
class WeekWidget : GlanceAppWidget() {

  // Exact: размер числа и фигуры подбираем под настоящую высоту.
  override val sizeMode = SizeMode.Exact

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val entryPoint =
        EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
    val data = entryPoint.loadWeek(context.resources.configuration.locales[0])
    scheduleWidgetUpdate(context, data.nextChange, WeekWidgetReceiver::class.java)

    provideContent { GlanceTheme { WeekContent(data) } }
  }
}

class WeekWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = WeekWidget()
}

internal class WeekDay(val date: LocalDate, val events: Int, val hasProject: Boolean)

internal class WeekData(
    val hasAccess: Boolean,
    val today: LocalDate,
    val days: List<WeekDay>,
    val nextChange: Instant,
)

private suspend fun WidgetEntryPoint.loadWeek(locale: Locale, now: Instant = Instant.now()): WeekData {
  permissionManager().refresh()
  val zone = settingsRepository().zoneFlow.first()
  val today = now.atZone(zone).toLocalDate()
  val midnight = today.plusDays(1).atStartOfDay(zone).toInstant()
  // Неделя как в календаре телефона: с понедельника или с воскресенья — по локали.
  val first = today.with(TemporalAdjusters.previousOrSame(WeekFields.of(locale).firstDayOfWeek))
  val granted = permissionManager().isGranted.value
  val days =
      (0L until 7L).map { offset ->
        val date = first.plusDays(offset)
        if (!granted) WeekDay(date, 0, false)
        else
            calendarRepository().getDayFlow(date).first().let {
              WeekDay(date, it.events.size, it.projects.isNotEmpty())
            }
      }
  return WeekData(granted, today, days, midnight)
}

/**
 * Точки занятости: 1–2 события — одна, 3–4 — две, больше — три. По точке на событие шкала
 * у загруженного календаря (с ежедневными повторами) сразу упирается в максимум.
 */
internal fun busyDots(events: Int): Int =
    when {
      events <= 0 -> 0
      events <= 2 -> 1
      events <= 4 -> 2
      else -> 3
    }

@Composable
private fun WeekContent(data: WeekData) {
  val context = LocalContext.current
  val size = LocalSize.current
  val locale = context.resources.configuration.locales[0]
  val density = context.resources.displayMetrics.density
  val innerH = size.height.value - 16f
  val columnW = (size.width.value - 16f) / 7f
  // Фигура «сегодня» — по меньшей стороне колонки, но не больше половины высоты.
  val shapeDp = minOf(columnW * 0.9f, innerH * 0.5f)
  val shapePx = (shapeDp * density).toInt().coerceAtLeast(1)

  Column(
      modifier =
          GlanceModifier.fillMaxSize()
              .appWidgetBackground()
              .background(GlanceTheme.colors.widgetBackground)
              .cornerRadius(android.R.dimen.system_app_widget_background_radius)
              .padding(8.dp)
              .clickable(actionStartActivity<MainActivity>()),
      verticalAlignment = Alignment.CenterVertically) {
        if (!data.hasAccess) {
          Text(
              text = context.getString(R.string.widget_no_access),
              maxLines = 2,
              modifier = GlanceModifier.padding(horizontal = 8.dp),
              style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
          return@Column
        }
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          data.days.forEach { day ->
            DayColumn(day, data.today, locale, columnW, shapeDp, shapePx)
          }
        }
      }
}

@Composable
private fun DayColumn(
    day: WeekDay,
    today: LocalDate,
    locale: Locale,
    widthDp: Float,
    shapeDp: Float,
    shapePx: Int,
) {
  val context = LocalContext.current
  val isToday = day.date == today
  val isPast = day.date.isBefore(today)
  // Прошедшие дни приглушаем, как историю проектов в приложении.
  val textColor =
      if (isPast) GlanceTheme.colors.outline else GlanceTheme.colors.onSurface
  val dotColor = if (isPast) GlanceTheme.colors.outline else GlanceTheme.colors.primary

  Column(
      modifier = GlanceModifier.width(widthDp.dp).fillMaxHeight(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = day.date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT_STANDALONE, locale).take(2),
            maxLines = 1,
            style =
                TextStyle(
                    color =
                        if (isToday) GlanceTheme.colors.primary
                        else GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp))
        Box(
            modifier = GlanceModifier.size(shapeDp.dp),
            contentAlignment = Alignment.Center) {
              val description =
                  DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                      .withLocale(locale)
                      .format(day.date)
              if (isToday) {
                Image(
                    provider = ImageProvider(dayShapeBitmap(day.date, shapePx)),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                    modifier = GlanceModifier.size(shapeDp.dp))
                Image(
                    provider = ImageProvider(dayNumberBitmap(context, day.date.dayOfMonth, shapePx)),
                    contentDescription = description,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
                    modifier = GlanceModifier.size(shapeDp.dp))
              } else {
                // Тем же RobotoFlex, что и «сегодня», только без фигуры.
                val number =
                    widgetTitle(
                        context = context,
                        text = day.date.dayOfMonth.toString(),
                        maxWidthDp = shapeDp,
                        maxHeightDp = shapeDp,
                        textSizeSp = (shapeDp * 0.5f).coerceIn(12f, 22f),
                        minTextSizeSp = 10f,
                        maxLines = 1)
                TitleImage(number, textColor, description)
              }
            }
        Spacer(GlanceModifier.height(3.dp))
        Row(modifier = GlanceModifier.height(5.dp), verticalAlignment = Alignment.CenterVertically) {
          repeat(busyDots(day.events)) { index ->
            if (index > 0) Spacer(GlanceModifier.width(2.dp))
            Box(
                modifier =
                    GlanceModifier.size(5.dp).background(dotColor).cornerRadius(2.5.dp)) {}
          }
        }
        Spacer(GlanceModifier.height(3.dp))
        // Полоска проекта во всю ширину колонки: у соседних дней сливается в ленту.
        Box(
            modifier =
                GlanceModifier.fillMaxWidth()
                    .height(3.dp)
                    .background(
                        if (day.hasProject) GlanceTheme.colors.secondary
                        else GlanceTheme.colors.widgetBackground)) {}
      }
}
