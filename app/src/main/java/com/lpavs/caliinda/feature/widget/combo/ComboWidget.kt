package com.lpavs.caliinda.feature.widget.combo

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
import androidx.glance.layout.ContentScale
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
import androidx.glance.unit.ColorProvider
import com.lpavs.caliinda.R
import com.lpavs.caliinda.app.MainActivity
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.ui.util.displayTitle
import com.lpavs.caliinda.feature.widget.TitleImage
import com.lpavs.caliinda.feature.widget.WidgetData
import com.lpavs.caliinda.feature.widget.WidgetEntryPoint
import com.lpavs.caliinda.feature.widget.date.dayNumberBitmap
import com.lpavs.caliinda.feature.widget.date.dayShapeBitmap
import com.lpavs.caliinda.feature.widget.loadWidgetData
import com.lpavs.caliinda.feature.widget.scheduleWidgetUpdate
import com.lpavs.caliinda.feature.widget.timeRange
import com.lpavs.caliinda.feature.widget.wavyProgressBitmap
import com.lpavs.caliinda.feature.widget.widgetShapeBitmap
import com.lpavs.caliinda.feature.widget.widgetTitle
import dagger.hilt.android.EntryPointAccessors
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.min

/**
 * Виджет «всё сразу»: слева дата в стиле виджета даты, справа главное на сегодня. Проект важнее
 * события: он идёт днями, событие — часами. Строкой ниже — событие под проектом или то, что
 * будет после текущего.
 */
class ComboWidget : GlanceAppWidget() {

  // Exact: картинки рисуем под настоящий размер виджета.
  override val sizeMode = SizeMode.Exact

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val entryPoint =
        EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
    val data = entryPoint.loadWidgetData()
    scheduleWidgetUpdate(context, data.nextChange, ComboWidgetReceiver::class.java)

    provideContent { GlanceTheme { ComboContent(data) } }
  }
}

class ComboWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = ComboWidget()
}

private const val PADDING_DP = 12f
private const val GAP_DP = 8f
private const val SECOND_CARD_DP = 52f

/** Цвета карточки: проект, идёт сейчас, будет потом, пусто — как в ленте приложения. */
private class CardColors(val container: ColorProvider, val content: ColorProvider)

@Composable
private fun ComboContent(data: WidgetData) {
  val context = LocalContext.current
  val size = LocalSize.current
  val innerW = size.width.value - PADDING_DP * 2
  val innerH = size.height.value - PADDING_DP * 2
  val dateW = min(innerH, innerW * 0.36f)
  val cardsW = innerW - dateW - GAP_DP

  Row(
      modifier =
          GlanceModifier.fillMaxSize()
              .appWidgetBackground()
              .background(GlanceTheme.colors.widgetBackground)
              .cornerRadius(android.R.dimen.system_app_widget_background_radius)
              .padding(PADDING_DP.dp)
              .clickable(actionStartActivity<MainActivity>()),
      verticalAlignment = Alignment.CenterVertically) {
        DateColumn(data, dateW, innerH)
        Spacer(GlanceModifier.width(GAP_DP.dp))
        Column(modifier = GlanceModifier.width(cardsW.dp).fillMaxHeight()) {
          val project = data.project
          val focus = data.focus
          when {
            !data.hasAccess ->
                MessageCard(context.getString(R.string.widget_no_access), cardsW, innerH)
            project != null -> {
              val second = focus != null && innerH >= 120f
              val mainH = if (second) innerH - GAP_DP - SECOND_CARD_DP else innerH
              MainCard(
                  event = project.event,
                  colors =
                      CardColors(
                          GlanceTheme.colors.secondaryContainer,
                          GlanceTheme.colors.onSecondaryContainer),
                  widthDp = cardsW,
                  heightDp = mainH) { color ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      val waveW = 72f
                      val density = context.resources.displayMetrics.density
                      Image(
                          provider =
                              ImageProvider(
                                  wavyProgressBitmap(
                                      (waveW * density).toInt(),
                                      (10 * density).toInt(),
                                      project.progress)),
                          contentDescription = null,
                          colorFilter = ColorFilter.tint(color),
                          modifier = GlanceModifier.width(waveW.dp).height(10.dp))
                      Spacer(GlanceModifier.width(8.dp))
                      Text(
                          text =
                              context.getString(
                                  R.string.project_day_of, project.day, project.totalDays),
                          maxLines = 1,
                          style = TextStyle(color = color, fontSize = 12.sp))
                    }
                  }
              if (second && focus != null) {
                Spacer(GlanceModifier.height(GAP_DP.dp))
                SecondCard(focus, data.focusIsNow, data, cardsW)
              }
            }
            focus != null -> {
              // Без проекта вторая строка — что будет после текущего события.
              val after = data.afterFocus?.takeIf { innerH >= 120f }
              MainCard(
                  event = focus,
                  colors = eventColors(data.focusIsNow),
                  widthDp = cardsW,
                  heightDp = if (after != null) innerH - GAP_DP - SECOND_CARD_DP else innerH) {
                      color ->
                    Text(
                        text = timeLine(context, focus, data),
                        maxLines = 1,
                        style = TextStyle(color = color, fontSize = 12.sp))
                  }
              if (after != null) {
                Spacer(GlanceModifier.height(GAP_DP.dp))
                SecondCard(after, isNow = false, data, cardsW)
              }
            }
            else ->
                MessageCard(context.getString(R.string.widget_no_more_events), cardsW, innerH)
          }
        }
      }
}

/** Дата как в виджете даты: число в фигуре дня, под ним месяц и день недели. */
@Composable
private fun DateColumn(data: WidgetData, widthDp: Float, heightDp: Float) {
  val context = LocalContext.current
  val locale = context.resources.configuration.locales[0]
  val density = context.resources.displayMetrics.density
  val today = data.today
  val shapeDp = min(widthDp * 0.86f, heightDp * 0.6f)
  val shapePx = (shapeDp * density).toInt().coerceAtLeast(1)
  val month =
      widgetTitle(
          context = context,
          text =
              DateTimeFormatter.ofPattern("LLLL", locale).format(today).replaceFirstChar {
                it.titlecase(locale)
              },
          maxWidthDp = widthDp,
          maxHeightDp = heightDp * 0.16f,
          textSizeSp = (shapeDp * 0.2f).coerceIn(14f, 22f),
          minTextSizeSp = 11f)
  val weekday =
      DateTimeFormatter.ofPattern("EEEE", locale).format(today).replaceFirstChar {
        it.titlecase(locale)
      }

  Column(
      modifier = GlanceModifier.width(widthDp.dp).fillMaxHeight(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
          Image(
              provider = ImageProvider(dayShapeBitmap(today, shapePx)),
              contentDescription = null,
              colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
              modifier = GlanceModifier.size(shapeDp.dp))
          Image(
              provider = ImageProvider(dayNumberBitmap(context, today.dayOfMonth, shapePx)),
              contentDescription =
                  DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(today),
              colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
              modifier = GlanceModifier.size(shapeDp.dp))
        }
        Spacer(GlanceModifier.height(4.dp))
        TitleImage(month, GlanceTheme.colors.onSurface, null)
        Text(
            text = weekday,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
      }
}

/** Главная карточка: фигура у края, как в ленте, крупное название и строка под ним. */
@Composable
private fun MainCard(
    event: EventDto,
    colors: CardColors,
    widthDp: Float,
    heightDp: Float,
    info: @Composable (ColorProvider) -> Unit,
) {
  val context = LocalContext.current
  val title =
      widgetTitle(
          context = context,
          text = event.displayTitle(context),
          maxWidthDp = widthDp - 28f,
          maxHeightDp = heightDp - 20f - INFO_LINE_DP,
          textSizeSp = 24f,
          minTextSizeSp = 14f)
  Box(
      modifier =
          GlanceModifier.width(widthDp.dp)
              .height(heightDp.dp)
              .background(colors.container)
              .cornerRadius(android.R.dimen.system_app_widget_inner_radius),
      contentAlignment = Alignment.CenterStart) {
        Image(
            provider = ImageProvider(
                    // Карточка тут выше, чем в ленте: фигуру уводим к правому краю, чтобы не лезла под название.
                    widgetShapeBitmap(event.id, widthDp / heightDp, size = 1.3f, inset = 0.1f)),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.content),
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxSize())
        Column(modifier = GlanceModifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
          TitleImage(title, colors.content, event.displayTitle(context))
          Spacer(GlanceModifier.height(4.dp))
          info(colors.content)
        }
      }
}

/** Событие под проектом — одной строкой: название и время. */
@Composable
private fun SecondCard(event: EventDto, isNow: Boolean, data: WidgetData, widthDp: Float) {
  val context = LocalContext.current
  val colors = eventColors(isNow)
  val title =
      widgetTitle(
          context = context,
          text = event.displayTitle(context),
          maxWidthDp = widthDp - 28f,
          maxHeightDp = 20f,
          textSizeSp = 14f,
          minTextSizeSp = 12f)
  Column(
      modifier =
          GlanceModifier.width(widthDp.dp)
              .height(SECOND_CARD_DP.dp)
              .background(colors.container)
              .cornerRadius(android.R.dimen.system_app_widget_inner_radius)
              .padding(horizontal = 14.dp),
      verticalAlignment = Alignment.CenterVertically) {
        TitleImage(title, colors.content, event.displayTitle(context))
        Text(
            text = timeLine(context, event, data),
            maxLines = 1,
            style = TextStyle(color = colors.content, fontSize = 11.sp))
      }
}

@Composable
private fun MessageCard(text: String, widthDp: Float, heightDp: Float) {
  Box(
      modifier =
          GlanceModifier.width(widthDp.dp)
              .height(heightDp.dp)
              .background(GlanceTheme.colors.surfaceVariant)
              .cornerRadius(android.R.dimen.system_app_widget_inner_radius)
              .padding(14.dp),
      contentAlignment = Alignment.CenterStart) {
        Text(
            text = text,
            maxLines = 3,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp))
      }
}

@Composable
private fun eventColors(isNow: Boolean) =
    if (isNow)
        CardColors(GlanceTheme.colors.tertiaryContainer, GlanceTheme.colors.onTertiaryContainer)
    else CardColors(GlanceTheme.colors.primaryContainer, GlanceTheme.colors.onPrimaryContainer)

private fun timeLine(context: Context, event: EventDto, data: WidgetData): String {
  val time = event.timeRange(context, data.zone)
  return if (event == data.focus && data.focusIsNow)
      "${context.getString(R.string.widget_now)} · $time"
  else time
}

// Строка под названием (время или прогресс) — с запасом на крупный системный шрифт.
private const val INFO_LINE_DP = 18f
