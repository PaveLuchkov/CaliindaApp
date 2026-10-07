package com.lpavs.caliinda.feature.widget.add

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import com.lpavs.caliinda.R
import com.lpavs.caliinda.app.LaunchRequests
import com.lpavs.caliinda.app.MainActivity
import com.lpavs.caliinda.feature.widget.date.polygonBitmap

/**
 * Кнопка «+» на рабочем столе: фигура в цвете темы, нажатие — сразу форма нового события.
 * Без подложки виджета — на обоях смотрится как иконка.
 */
class AddEventWidget : GlanceAppWidget() {

  override val sizeMode = SizeMode.Exact

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    provideContent { GlanceTheme { AddButton() } }
  }
}

class AddEventWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = AddEventWidget()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddButton() {
  val context = LocalContext.current
  val size = LocalSize.current
  val density = context.resources.displayMetrics.density
  val sideDp = minOf(size.width.value, size.height.value) * 0.9f
  val sidePx = (sideDp * density).toInt().coerceAtLeast(1)
  // CLEAR_TOP + singleTop: при открытом приложении интент придёт в onNewIntent, без второй копии.
  val intent =
      Intent(context, MainActivity::class.java)
          .setAction(LaunchRequests.ACTION_CREATE_EVENT)
          .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

  Box(
      modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(intent)),
      contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(polygonBitmap(MaterialShapes.Cookie9Sided, sidePx)),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            modifier = GlanceModifier.size(sideDp.dp))
        Image(
            provider = ImageProvider(plusBitmap(sidePx)),
            contentDescription = context.getString(R.string.create_event),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
            modifier = GlanceModifier.size(sideDp.dp))
      }
}

/** «+» со скруглёнными концами по центру квадрата [sidePx], белым — под tint. */
private fun plusBitmap(sidePx: Int): Bitmap {
  val paint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = sidePx * 0.09f
        strokeCap = Paint.Cap.ROUND
      }
  val center = sidePx / 2f
  val half = sidePx * 0.18f
  return Bitmap.createBitmap(sidePx, sidePx, Bitmap.Config.ARGB_8888).also {
    Canvas(it).apply {
      drawLine(center - half, center, center + half, center, paint)
      drawLine(center, center - half, center, center + half, paint)
    }
  }
}
