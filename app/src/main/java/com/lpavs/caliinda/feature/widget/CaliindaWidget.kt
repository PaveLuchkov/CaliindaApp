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
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.app.MainActivity
import dagger.hilt.android.EntryPointAccessors
import java.time.format.DateTimeFormatter

/**
 * Виджет в одну строку: карточка того, что идёт сейчас или будет следующим, на всю площадь —
 * как карточка события в приложении.
 */
class CaliindaWidget : GlanceAppWidget() {

  // Exact: картинку с фигурой рисуем под настоящие пропорции виджета.
  override val sizeMode = SizeMode.Exact

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val entryPoint =
        EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
    val data = entryPoint.loadWidgetData()
    scheduleWidgetUpdate(context, data.nextChange)

    provideContent { GlanceTheme { WidgetCard(data) } }
  }
}

@Composable
private fun WidgetCard(data: WidgetData) {
  val context = LocalContext.current
  val size = LocalSize.current
  val focus = data.focus
  val isNow = data.focusIsNow
  val container =
      when {
        focus == null -> GlanceTheme.colors.surfaceVariant
        isNow -> GlanceTheme.colors.tertiaryContainer
        else -> GlanceTheme.colors.primaryContainer
      }
  val content =
      when {
        focus == null -> GlanceTheme.colors.onSurfaceVariant
        isNow -> GlanceTheme.colors.onTertiaryContainer
        else -> GlanceTheme.colors.onPrimaryContainer
      }

  Box(
      modifier =
          GlanceModifier.fillMaxSize()
              .background(container)
              .cornerRadius(20.dp)
              .clickable(actionStartActivity<MainActivity>()),
      contentAlignment = Alignment.CenterStart) {
        if (focus != null) {
          Image(
              provider = ImageProvider(widgetShapeBitmap(focus.id, size.width / size.height)),
              contentDescription = null,
              colorFilter = ColorFilter.tint(content),
              contentScale = ContentScale.FillBounds,
              modifier = GlanceModifier.fillMaxSize())
        }
        Column(modifier = GlanceModifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
          when {
            !data.hasAccess -> Line(context.getString(R.string.widget_no_access), content, 14)
            focus == null -> Line(context.getString(R.string.widget_no_more_events), content, 15)
            else -> {
              val locale = context.resources.configuration.locales[0]
              val formatter =
                  DateTimeFormatter.ofPattern(
                      if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", locale)
              val time =
                  "${focus.startTime.atZone(data.zone).format(formatter)} – " +
                      focus.endTime.atZone(data.zone).format(formatter)
              // Название — картинкой нашим RobotoFlex: жирно и шире обычного, как в приложении.
              val title =
                  widgetTitle(
                      context = context,
                      text = focus.summary,
                      maxWidthDp = size.width.value - 32f,
                      textSizeSp = 24f)
              Image(
                  provider = ImageProvider(title.bitmap),
                  contentDescription = focus.summary,
                  colorFilter = ColorFilter.tint(content),
                  modifier = GlanceModifier.width(title.widthDp.dp).height(title.heightDp.dp))
              Text(
                  text = if (isNow) "${context.getString(R.string.widget_now)} · $time" else time,
                  maxLines = 1,
                  style = TextStyle(color = content, fontSize = 13.sp))
            }
          }
        }
      }
}

@Composable
private fun Line(text: String, color: androidx.glance.unit.ColorProvider, sizeSp: Int) {
  Text(text = text, maxLines = 2, style = TextStyle(color = color, fontSize = sizeSp.sp))
}
