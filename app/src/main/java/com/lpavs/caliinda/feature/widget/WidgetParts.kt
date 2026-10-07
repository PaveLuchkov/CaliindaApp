package com.lpavs.caliinda.feature.widget

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Box
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
