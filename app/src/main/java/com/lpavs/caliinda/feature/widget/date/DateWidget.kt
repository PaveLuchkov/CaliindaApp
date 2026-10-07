package com.lpavs.caliinda.feature.widget.date

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
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
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import com.lpavs.caliinda.app.MainActivity
import com.lpavs.caliinda.feature.widget.WidgetEntryPoint
import com.lpavs.caliinda.feature.widget.scheduleWidgetUpdate
import com.lpavs.caliinda.feature.widget.widgetTitle
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Квадратный виджет даты: крупное число в фигуре дня, под ним месяц, а на заднем плане
 * выглядывает эмодзи сезона или праздника. Событий не показывает — поэтому работает и без
 * доступа к календарю.
 */
class DateWidget : GlanceAppWidget() {

  // Exact: картинки рисуем под настоящий размер виджета.
  override val sizeMode = SizeMode.Exact

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val entryPoint =
        EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
    val zone = entryPoint.settingsRepository().zoneFlow.first()
    val today = ZonedDateTime.now(zone).toLocalDate()
    scheduleWidgetUpdate(
        context, today.plusDays(1).atStartOfDay(zone).toInstant(), DateWidgetReceiver::class.java)

    provideContent { GlanceTheme { DateContent(today, zone) } }
  }
}

class DateWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = DateWidget()
}

@Composable
private fun DateContent(today: LocalDate, zone: ZoneId) {
  val context = LocalContext.current
  val size = LocalSize.current
  val density = context.resources.displayMetrics.density
  val locale = context.resources.configuration.locales[0]
  val side = minOf(size.width.value, size.height.value)
  // Фигура с числом — главное, месяц — подпись под ней.
  val shapeDp = side * 0.62f
  val shapePx = (shapeDp * density).toInt().coerceAtLeast(1)
  val monthName =
      DateTimeFormatter.ofPattern("LLLL", locale).format(today).replaceFirstChar {
        it.titlecase(locale)
      }
  val month =
      widgetTitle(
          context = context,
          text = monthName,
          maxWidthDp = size.width.value - 24f,
          maxHeightDp = side * 0.2f,
          textSizeSp = (side * 0.13f).coerceIn(14f, 30f),
          minTextSizeSp = 12f)
  val fullDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(today)

  Box(
      modifier =
          GlanceModifier.fillMaxSize()
              .appWidgetBackground()
              .background(GlanceTheme.colors.primaryContainer)
              .cornerRadius(android.R.dimen.system_app_widget_background_radius)
              .clickable(actionStartActivity<MainActivity>()),
      contentAlignment = Alignment.Center) {
        Image(
            provider =
                ImageProvider(
                    dayEmojiBitmap(
                        DayEmoji.forDate(today, locale, zone),
                        (size.width.value * density).toInt().coerceAtLeast(1),
                        (size.height.value * density).toInt().coerceAtLeast(1))),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxSize())
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Box(contentAlignment = Alignment.Center) {
            Image(
                provider = ImageProvider(dayShapeBitmap(today, shapePx)),
                contentDescription = null,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(shapeDp.dp))
            Image(
                provider = ImageProvider(dayNumberBitmap(context, today.dayOfMonth, shapePx)),
                contentDescription = fullDate,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
                modifier = GlanceModifier.size(shapeDp.dp))
          }
          Image(
              provider = ImageProvider(month.glyphs),
              contentDescription = null,
              colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
              modifier = GlanceModifier.width(month.widthDp.dp).height(month.heightDp.dp))
        }
      }
}
