package com.lpavs.caliinda.feature.widget

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

/** Название-картинка: буквы в цвет темы, эмодзи поверх в своих цветах. */
@Composable
fun TitleImage(title: WidgetTitle, color: ColorProvider, contentDescription: String?) {
  val modifier = GlanceModifier.width(title.widthDp.dp).height(title.heightDp.dp)
  Box {
    Image(
        provider = ImageProvider(title.glyphs),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(color),
        modifier = modifier)
    title.emoji?.let {
      Image(provider = ImageProvider(it), contentDescription = null, modifier = modifier)
    }
  }
}

/** «14:00 – 15:00» в формате часов телефона. */
fun EventDto.timeRange(context: Context, zone: ZoneId): String {
  val locale = context.resources.configuration.locales[0]
  val formatter =
      DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", locale)
  return "${startTime.atZone(zone).format(formatter)} – ${endTime.atZone(zone).format(formatter)}"
}

/**
 * Строка идущего события: волнистый прогресс, как у лент проектов в приложении, и «ещё 25 мин ·
 * 21:00 – 22:00». Слово «сейчас» не нужно — волна и так говорит, что событие идёт.
 */
@Composable
fun NowLine(
    event: EventDto,
    now: Instant,
    zone: ZoneId,
    color: ColorProvider,
    fontSize: TextUnit,
    waveWidthDp: Float = 52f,
    /** В узкой карточке диапазон не влезет — хватит «ещё 25 мин». */
    showRange: Boolean = true,
) {
  val context = LocalContext.current
  val density = context.resources.displayMetrics.density
  val total = Duration.between(event.startTime, event.endTime).toMillis().coerceAtLeast(1)
  val passed = Duration.between(event.startTime, now).toMillis()
  val waveHeightDp = 8f
  Row(verticalAlignment = Alignment.CenterVertically) {
    Image(
        provider =
            ImageProvider(
                wavyProgressBitmap(
                    (waveWidthDp * density).toInt(),
                    (waveHeightDp * density).toInt(),
                    passed.toFloat() / total)),
        contentDescription = null,
        colorFilter = ColorFilter.tint(color),
        modifier = GlanceModifier.width(waveWidthDp.dp).height(waveHeightDp.dp))
    Spacer(GlanceModifier.width(6.dp))
    Text(
        text =
            timeLeft(context, event.endTime, now) +
                if (showRange) " · ${event.timeRange(context, zone)}" else "",
        maxLines = 1,
        style = TextStyle(color = color, fontSize = fontSize))
  }
}

/** «ещё 25 мин», «ещё 1 ч 20 мин»: минуты округляем вверх — «ещё 0 мин» не бывает. */
fun timeLeft(context: Context, end: Instant, now: Instant): String {
  val minutes = ceil(Duration.between(now, end).toSeconds() / 60.0).toInt().coerceAtLeast(1)
  val hours = minutes / 60
  val rest = minutes % 60
  return when {
    hours == 0 -> context.getString(R.string.widget_left_minutes, minutes)
    rest == 0 -> context.getString(R.string.widget_left_hours, hours)
    else -> context.getString(R.string.widget_left_hours_minutes, hours, rest)
  }
}
