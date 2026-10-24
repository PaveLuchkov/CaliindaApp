package com.lpavs.caliinda.feature.calendar.presentation.components.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.utils.googleCalendarLink
import com.lpavs.caliinda.core.ui.components.QrCode
import com.lpavs.caliinda.core.ui.theme.AppMotion
import com.lpavs.caliinda.core.ui.util.displayTitle
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * «Поделиться»: QR в фигуре — навести камеру и добавить событие к себе; под ним — скопировать
 * ссылку или отправить через системную шторку (текст + .ics).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShareEventSheet(event: EventDto, zoneId: ZoneId, onDismiss: () -> Unit) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val link = remember(event, zoneId) { googleCalendarLink(event, zoneId) }
  // В QR — короткая ссылка без места: длинная превращает код в мелкую рябь.
  val qrLink = remember(event, zoneId) { googleCalendarLink(event, zoneId, compact = true) }
  val whenText = remember(event, zoneId) { context.eventWhenText(event, zoneId) }
  var copied by remember { mutableStateOf(false) }
  LaunchedEffect(copied) {
    if (copied) {
      delay(COPIED_MS)
      copied = false
    }
  }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
              text = event.displayTitle(),
              style = typography.headlineSmallEmphasized,
              textAlign = TextAlign.Center)
          Spacer(Modifier.height(4.dp))
          Text(
              text = whenText,
              style = typography.bodyMedium,
              color = colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center)
          Spacer(Modifier.height(24.dp))

          // Сканерам нужны тёмные модули на светлом: в тёмной теме роли меняем местами.
          val dark = isSystemInDarkTheme()
          val background = if (dark) colorScheme.onPrimaryContainer else colorScheme.primaryContainer
          val modules = if (dark) colorScheme.primaryContainer else colorScheme.onPrimaryContainer
          // Фигура «докручивается» пружиной при открытии; сам код не вращаем — его читают камерой.
          val turn = remember { Animatable(-45f) }
          val scale = remember { Animatable(0.85f) }
          LaunchedEffect(Unit) {
            launch { turn.animateTo(0f, AppMotion.slowSpatialSpec()) }
            scale.animateTo(1f, AppMotion.slowSpatialSpec())
          }
          Box(contentAlignment = Alignment.Center, modifier = Modifier.size(SHAPE_SIZE)) {
            Box(
                Modifier.matchParentSize()
                    .graphicsLayer {
                      rotationZ = turn.value
                      scaleX = scale.value
                      scaleY = scale.value
                    }
                    .clip(MaterialShapes.Cookie12Sided.toShape())
                    .background(background))
            // Квадрат кода вписан во внутренний круг фигуры, остаток — тихая зона.
            QrCode(text = qrLink, color = modules, modifier = Modifier.size(SHAPE_SIZE * QR_FRACTION))
          }
          Spacer(Modifier.height(16.dp))
          Text(
              text = stringResource(R.string.share_qr_hint),
              style = typography.bodyMedium,
              color = colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center)
          Spacer(Modifier.height(24.dp))

          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                      context
                          .getSystemService(ClipboardManager::class.java)
                          .setPrimaryClip(ClipData.newPlainText(event.displayTitle(context), link))
                      haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                      copied = true
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                      AnimatedContent(
                          targetState = copied,
                          transitionSpec = {
                            fadeIn(AppMotion.fastEffectsSpec()) togetherWith fadeOut(AppMotion.fastEffectsSpec())
                          },
                          label = "copyLink") { done ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                              Icon(
                                  if (done) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                  contentDescription = null)
                              Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                              Text(stringResource(if (done) R.string.link_copied else R.string.copy_link))
                            }
                          }
                    }
                Button(
                    onClick = { context.sendEvent(event, zoneId, link) },
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                      Icon(Icons.Rounded.Send, contentDescription = null)
                      Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                      Text(stringResource(R.string.share_send))
                    }
              }
        }
  }
}

private val SHAPE_SIZE = 300.dp
/** Внутренний радиус Cookie12 ≈ 0,8 внешнего; квадрат в таком круге — ≈ 0,56 стороны фигуры. */
private const val QR_FRACTION = 0.58f
private const val COPIED_MS = 2000L
