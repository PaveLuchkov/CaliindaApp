package com.lpavs.caliinda.core.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Открыть место в картах: geo-ссылку понимают Google Карты, Яндекс, 2ГИС и другие — система
 * предложит выбрать. Карт нет совсем — поиск Google Maps в браузере.
 */
fun Context.openInMaps(place: String) {
  val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(place)))
  try {
    startActivity(geo)
  } catch (_: ActivityNotFoundException) {
    runCatching {
      startActivity(
          Intent(
              Intent.ACTION_VIEW,
              Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(place))))
    }
  }
}
