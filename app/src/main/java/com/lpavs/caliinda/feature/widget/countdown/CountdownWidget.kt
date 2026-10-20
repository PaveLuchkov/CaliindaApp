package com.lpavs.caliinda.feature.widget.countdown

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.app.MainActivity
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.ui.util.displayTitle
import com.lpavs.caliinda.feature.widget.TitleImage
import com.lpavs.caliinda.feature.widget.WidgetEntryPoint
import com.lpavs.caliinda.feature.widget.openEventAction
import com.lpavs.caliinda.feature.widget.date.dayNumberBitmap
import com.lpavs.caliinda.feature.widget.date.emojiInShapeBitmap
import com.lpavs.caliinda.feature.widget.date.polygonBitmap
import com.lpavs.caliinda.feature.widget.emojiRanges
import com.lpavs.caliinda.feature.widget.scheduleWidgetUpdate
import com.lpavs.caliinda.feature.widget.widgetTitle
import dagger.hilt.android.EntryPointAccessors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.first

/** Выбранное событие: id (у серии — мастер-события) и название на случай, если его удалят. */
internal val COUNTDOWN_EVENT_ID = longPreferencesKey("countdown_event_id")
internal val COUNTDOWN_TITLE = stringPreferencesKey("countdown_title")

// Строка подписи над названием («дней до») — с запасом на крупный системный шрифт.
private const val CAPTION_DP = 18f

/** Сколько вперёд ищем выбранное событие: ежегодное найдётся всегда. */
internal const val COUNTDOWN_LOOKAHEAD_DAYS = 400L

/**
 * Обратный отсчёт до события, которое человек выбрал сам: «12 дней до ✈️ Отпуска». Число — в
 * фигуре события, как в ленте. Выбор — в [CountdownConfigActivity], сменить можно в настройках
 * виджета на рабочем столе.
 */
class CountdownWidget : GlanceAppWidget() {

  // Exact: картинки рисуем под настоящий размер виджета.
  override val sizeMode = SizeMode.Exact

  override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val entryPoint =
        EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
    val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

    provideContent {
      val prefs = currentState<Preferences>()
      val eventId = prefs[COUNTDOWN_EVENT_ID]
      val title = prefs[COUNTDOWN_TITLE]
      // Читаем календарь заново, когда выбрали другое событие: сессия виджета могла остаться жива.
      val data by
          produceState<CountdownData?>(null, eventId, title) {
            value =
                entryPoint.loadCountdown(eventId, title).also {
                  scheduleWidgetUpdate(context, it.nextChange, CountdownWidgetReceiver::class.java)
                }
          }
      GlanceTheme {
        // Пока читаем календарь — пустой фон, а не прозрачная дыра на рабочем столе.
        data?.let { CountdownContent(it, appWidgetId) }
            ?: Box(
                modifier =
                    GlanceModifier.fillMaxSize()
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.tertiaryContainer)
                        .cornerRadius(android.R.dimen.system_app_widget_background_radius)) {}
      }
    }
  }
}

class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = CountdownWidget()
}

internal class CountdownData(
    val hasAccess: Boolean,
    val countdown: Countdown,
    val zone: ZoneId,
    val nextChange: Instant,
)

private suspend fun WidgetEntryPoint.loadCountdown(
    eventId: Long?,
    savedTitle: String?,
    now: Instant = Instant.now(),
): CountdownData {
  permissionManager().refresh()
  val zone = settingsRepository().zoneFlow.first()
  val today = now.atZone(zone).toLocalDate()
  val midnight = today.plusDays(1).atStartOfDay(zone).toInstant()
  if (!permissionManager().isGranted.value) {
    return CountdownData(false, Countdown.NotSet, zone, midnight)
  }
  val event =
      eventId?.let { id ->
        calendarRepository().getUpcomingFlow(today, COUNTDOWN_LOOKAHEAD_DAYS).first().firstOrNull {
          it.eventId == id || it.originalEventId == id
        }
      }
  val nextChange =
      listOfNotNull(event?.startTime, event?.endTime, midnight)
          .filter { it.isAfter(now) }
          .minOrNull() ?: midnight
  return CountdownData(true, countdownOf(event, savedTitle, now, zone), zone, nextChange)
}

@Composable
private fun CountdownContent(data: CountdownData, appWidgetId: Int) {
  val context = LocalContext.current
  val countdown = data.countdown
  // Пока выбирать нечего или событие прошло — нажатие ведёт к выбору, иначе в приложение.
  val onClick: Action =
      if (countdown is Countdown.NotSet || countdown is Countdown.Gone)
          actionStartActivity(
              Intent(context, CountdownConfigActivity::class.java)
                  .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                  // Своя «задача» на каждый виджет, иначе система переиспользует интент.
                  .setData(android.net.Uri.parse("caliinda://countdown/$appWidgetId")))
      else {
        // Идёт отсчёт — открываем само событие: его день и подробности.
        val event =
            when (countdown) {
              is Countdown.Ahead -> countdown.event
              is Countdown.Today -> countdown.event
              is Countdown.Ongoing -> countdown.event
              else -> null
            }
        event?.let {
          openEventAction(context, it, data.zone, java.time.LocalDate.now(data.zone))
        } ?: actionStartActivity<MainActivity>()
      }

  Box(
      modifier =
          GlanceModifier.fillMaxSize()
              .appWidgetBackground()
              .background(GlanceTheme.colors.tertiaryContainer)
              .cornerRadius(android.R.dimen.system_app_widget_background_radius)
              .clickable(onClick),
      contentAlignment = Alignment.Center) {
        when {
          !data.hasAccess -> Message(context.getString(R.string.widget_no_access))
          countdown is Countdown.NotSet -> Message(context.getString(R.string.countdown_pick))
          countdown is Countdown.Gone ->
              Message(context.getString(R.string.countdown_passed, countdown.title))
          else -> CountdownBody(data)
        }
      }
}

@Composable
private fun Message(text: String) {
  Text(
      text = text,
      maxLines = 4,
      modifier = GlanceModifier.padding(16.dp),
      style =
          TextStyle(
              color = GlanceTheme.colors.onTertiaryContainer,
              fontSize = 14.sp,
              textAlign = TextAlign.Center))
}

@Composable
private fun CountdownBody(data: CountdownData) {
  val context = LocalContext.current
  val size = LocalSize.current
  val density = context.resources.displayMetrics.density
  val width = size.width.value
  val height = size.height.value
  val side = minOf(width, height)
  val wide = width >= height * 1.6f
  // В 1×1 остаётся одна фигура с числом.
  val showText = wide || side >= 100f
  val shapeDp =
      when {
        wide -> height * 0.62f
        showText -> side * 0.56f
        else -> side * 0.75f
      }
  val shapePx = (shapeDp * density).toInt().coerceAtLeast(1)

  val (event, center, caption) = describe(context, data)
  val title = event.displayTitle(context)
  val textWidth = if (wide) width - shapeDp - 30f else width - 24f
  // «дней до» — строка, «Сегодня в 23:20» в узком виджете — две.
  val captionLines = if (wide && caption.length > 14) 2 else 1
  val titleImage =
      if (!showText) null
      else
          widgetTitle(
              context = context,
              text = title,
              maxWidthDp = textWidth,
              // В низком виджете название занимает всю высоту под подписью — до трёх строк.
              maxHeightDp = if (wide) height - 24f - CAPTION_DP * captionLines else side * 0.24f,
              textSizeSp = if (wide) (height * 0.2f).coerceIn(14f, 22f) else 18f,
              minTextSizeSp = 11f,
              maxLines = if (wide) 3 else 2)
  val number = center.toIntOrNull()

  val figure =
      @Composable {
        Box(contentAlignment = Alignment.Center) {
          Image(
              provider =
                  ImageProvider(
                      polygonBitmap(countdownPolygon(event.countdownKey), shapePx)),
              contentDescription = null,
              colorFilter = ColorFilter.tint(GlanceTheme.colors.tertiary),
              modifier = GlanceModifier.size(shapeDp.dp))
          if (number != null) {
            Image(
                provider = ImageProvider(dayNumberBitmap(context, number, shapePx, fill = 0.9f)),
                contentDescription = "$caption $title",
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiary),
                modifier = GlanceModifier.size(shapeDp.dp))
          } else {
            Image(
                provider = ImageProvider(emojiInShapeBitmap(center, shapePx)),
                contentDescription = "$caption $title",
                modifier = GlanceModifier.size(shapeDp.dp))
          }
        }
      }
  val texts =
      @Composable { alignment: Alignment.Horizontal ->
        Column(
            modifier = GlanceModifier.width(textWidth.dp), horizontalAlignment = alignment) {
          Text(
              text = caption,
              maxLines = captionLines,
              style = TextStyle(color = GlanceTheme.colors.onTertiaryContainer, fontSize = 13.sp))
          titleImage?.let { TitleImage(it, GlanceTheme.colors.onTertiaryContainer, null) }
        }
      }

  when {
    wide ->
        Row(verticalAlignment = Alignment.CenterVertically) {
          figure()
          Spacer(GlanceModifier.width(10.dp))
          texts(Alignment.Start)
        }
    showText ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          figure()
          Spacer(GlanceModifier.height(6.dp))
          texts(Alignment.CenterHorizontally)
        }
    else -> figure()
  }
}

/** Событие, что в центре фигуры (число дней или эмодзи) и подпись над названием. */
private fun describe(context: Context, data: CountdownData): Triple<EventDto, String, String> =
    when (val c = data.countdown) {
      is Countdown.Ahead ->
          Triple(
              c.event,
              c.days.toString(),
              context.resources.getQuantityString(R.plurals.countdown_days_until, c.days))
      is Countdown.Today -> {
        val caption =
            if (c.event.isAllDay) context.getString(R.string.countdown_today)
            else
                context.getString(
                    R.string.countdown_today_at,
                    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                        .withLocale(context.resources.configuration.locales[0])
                        .format(c.event.startTime.atZone(data.zone)))
        Triple(c.event, leadingEmoji(c.event.summary) ?: "⏳", caption)
      }
      is Countdown.Ongoing -> {
        val caption =
            if (c.totalDays > 1)
                context.getString(R.string.countdown_ongoing_day, c.day, c.totalDays)
            else context.getString(R.string.countdown_ongoing)
        Triple(c.event, leadingEmoji(c.event.summary) ?: "🎉", caption)
      }
      else -> error("describe() только для идущего или предстоящего события")
    }

/** Эмодзи в начале названия («✈️ Отпуск») — им и заменяем число, когда день настал. */
internal fun leadingEmoji(title: String): String? =
    emojiRanges(title).firstOrNull()?.takeIf { it.first == 0 }?.let { title.substring(it) }
