package com.lpavs.caliinda.core.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.utils.EventPlace
import com.lpavs.caliinda.core.data.utils.MeetingService

/** Подпись места: адрес как есть, созвон — названием сервиса, прочая ссылка — доменом. */
@Composable
fun placeLabel(place: EventPlace): String =
    when (place) {
      is EventPlace.Address -> place.text
      is EventPlace.Link -> place.service?.let { stringResource(it.nameRes()) } ?: place.host
    }

fun placeIcon(place: EventPlace): ImageVector =
    when (place) {
      is EventPlace.Address -> Icons.Filled.LocationOn
      is EventPlace.Link -> if (place.service != null) Icons.Rounded.Videocam else Icons.Rounded.Link
    }

/** Адрес — в карты, ссылку — тому, кто её откроет: приложению Zoom/Meet или браузеру. */
fun Context.openPlace(place: EventPlace) {
  when (place) {
    is EventPlace.Address -> openInMaps(place.text)
    is EventPlace.Link ->
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(place.url))) }
  }
}

private fun MeetingService.nameRes(): Int =
    when (this) {
      MeetingService.ZOOM -> R.string.meeting_zoom
      MeetingService.MEET -> R.string.meeting_meet
      MeetingService.TEAMS -> R.string.meeting_teams
      MeetingService.TELEMOST -> R.string.meeting_telemost
      MeetingService.WEBEX -> R.string.meeting_webex
      MeetingService.DISCORD -> R.string.meeting_discord
      MeetingService.TELEGRAM -> R.string.meeting_telegram
      MeetingService.WHEREBY -> R.string.meeting_whereby
    }
