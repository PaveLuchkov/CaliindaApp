package com.lpavs.caliinda.feature.widget

import android.content.Context
import android.text.format.DateFormat
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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.app.MainActivity
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import dagger.hilt.android.EntryPointAccessors
import java.time.format.DateTimeFormatter

/** Виджет «день»: что идёт сейчас или будет следующим, дальше по списку и идущие проекты. */
class CaliindaWidget : GlanceAppWidget() {

  // Exact, а не Responsive: строки под карточкой считаются от настоящей высоты виджета.
  override val sizeMode = SizeMode.Exact

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val entryPoint =
        EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
    val data = entryPoint.loadWidgetData()
    scheduleWidgetUpdate(context, data.nextChange)
    val shape = data.focus?.let { widgetShapeBitmap(it.id) }

    provideContent { GlanceTheme { WidgetContent(data, shape) } }
  }
}

private val HEADER_AND_CARD_HEIGHT = 24.dp + 26.dp + 96.dp
private val ROW_HEIGHT = 62.dp

@Composable
private fun WidgetContent(data: WidgetData, shape: android.graphics.Bitmap?) {
  val context = LocalContext.current
  val height = LocalSize.current.height
  val locale = context.resources.configuration.locales[0]
  val timeFormatter =
      DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", locale)
  val time = { event: EventDto ->
    "${event.startTime.atZone(data.zone).format(timeFormatter)} – ${event.endTime.atZone(data.zone).format(timeFormatter)}"
  }

  Column(
      modifier =
          GlanceModifier.fillMaxSize()
              .background(GlanceTheme.colors.widgetBackground)
              .cornerRadius(24.dp)
              .padding(12.dp)
              .clickable(actionStartActivity<MainActivity>())) {
        Text(
            text =
                data.today
                    .format(DateTimeFormatter.ofPattern("EEE, d MMMM", locale))
                    .replaceFirstChar { it.titlecase(locale) },
            style =
                TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium),
            modifier = GlanceModifier.padding(start = 4.dp, bottom = 6.dp))

        when {
          !data.hasAccess -> Message(context.getString(R.string.widget_no_access))
          data.focus == null -> Message(context.getString(R.string.widget_no_more_events))
          else -> FocusCard(data.focus, time(data.focus), data.focusIsNow, shape)
        }

        // Сколько строк помещается под карточкой: сначала следующие события, остаток — проекты.
        // Высота за вычетом отступов, даты и главной карточки, делённая на высоту строки проекта
        // (самой высокой): лучше на строку меньше, чем обрезанная снизу.
        val rows = ((height - HEADER_AND_CARD_HEIGHT) / ROW_HEIGHT).toInt().coerceAtLeast(0)
        val upcoming = data.upcoming.take(rows)
        upcoming.forEach { event ->
          Spacer(GlanceModifier.height(6.dp))
          UpcomingRow(event.summary, event.startTime.atZone(data.zone).format(timeFormatter))
        }
        data.projects.take(rows - upcoming.size).forEach { project ->
          Spacer(GlanceModifier.height(6.dp))
          ProjectRow(project)
        }
      }
}

@Composable
private fun FocusCard(event: EventDto, time: String, isNow: Boolean, shape: android.graphics.Bitmap?) {
  val context = LocalContext.current
  val container = if (isNow) GlanceTheme.colors.tertiaryContainer else GlanceTheme.colors.primaryContainer
  val content = if (isNow) GlanceTheme.colors.onTertiaryContainer else GlanceTheme.colors.onPrimaryContainer
  Box(
      modifier = GlanceModifier.fillMaxWidth().background(container).cornerRadius(20.dp),
      contentAlignment = Alignment.CenterEnd) {
        if (shape != null) {
          // Фигура как на карточках приложения: крупная, у правого края, частично за кадром.
          Image(
              provider = ImageProvider(shape),
              contentDescription = null,
              colorFilter = ColorFilter.tint(content),
              modifier = GlanceModifier.size(88.dp))
        }
        Column(modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
          Text(
              text = event.summary,
              maxLines = 2,
              style = TextStyle(color = content, fontSize = 20.sp, fontWeight = FontWeight.Bold))
          Spacer(GlanceModifier.height(2.dp))
          Text(
              text = if (isNow) "${context.getString(R.string.widget_now)} · $time" else time,
              maxLines = 1,
              style = TextStyle(color = content, fontSize = 13.sp))
        }
      }
}

@Composable
private fun UpcomingRow(title: String, start: String) {
  Row(
      modifier =
          GlanceModifier.fillMaxWidth()
              .background(GlanceTheme.colors.surfaceVariant)
              .cornerRadius(16.dp)
              .padding(horizontal = 14.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = start,
            style =
                TextStyle(
                    color = GlanceTheme.colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.width(10.dp))
        Text(
            text = title,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp))
      }
}

@Composable
private fun ProjectRow(project: WidgetProject) {
  val context = LocalContext.current
  Column(
      modifier =
          GlanceModifier.fillMaxWidth()
              .background(GlanceTheme.colors.secondaryContainer)
              .cornerRadius(16.dp)
              .padding(horizontal = 14.dp, vertical = 8.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = project.event.summary,
              maxLines = 1,
              style =
                  TextStyle(
                      color = GlanceTheme.colors.onSecondaryContainer,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Medium),
              modifier = GlanceModifier.defaultWeight())
          Text(
              text = context.getString(R.string.project_day_of, project.dayNumber, project.totalDays),
              style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 12.sp))
        }
        Spacer(GlanceModifier.height(6.dp))
        LinearProgressIndicator(
            progress = (project.dayNumber.toFloat() / project.totalDays).coerceIn(0f, 1f),
            modifier = GlanceModifier.fillMaxWidth().height(4.dp),
            color = GlanceTheme.colors.secondary,
            backgroundColor = GlanceTheme.colors.surfaceVariant)
      }
}

@Composable
private fun Message(text: String) {
  Box(
      modifier =
          GlanceModifier.fillMaxWidth()
              .background(GlanceTheme.colors.surfaceVariant)
              .cornerRadius(20.dp)
              .padding(16.dp)) {
        Text(
            text = text,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp))
      }
}

