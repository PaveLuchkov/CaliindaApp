package com.lpavs.caliinda.feature.calendar.presentation.components.dialogs

import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.eventStarShape
import com.lpavs.caliinda.core.ui.util.projectShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import android.view.Gravity
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.ui.platform.LocalContext
import com.lpavs.caliinda.core.ui.util.openInMaps
import java.time.ZoneId
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lpavs.caliinda.core.ui.util.formatRRule
import com.lpavs.caliinda.feature.calendar.presentation.model.EventDetailsUiModel
import com.lpavs.caliinda.core.data.calendar.model.EventDto

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomEventDetailsDialog(
    event: EventDetailsUiModel,
    userTimeZone: ZoneId,
    onDismissRequest: () -> Unit,
    onEdit: (EventDto) -> Unit,
    onDelete: (EventDto) -> Unit,
) {
  Dialog(
      onDismissRequest = onDismissRequest,
      properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // Карточка прижата к низу: верх зависит от длины текста, а кнопки снизу всегда в одной
        // точке экрана — и под большим пальцем.
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setGravity(Gravity.BOTTOM) }

        val container = if (event.isCurrent) colorScheme.tertiaryContainer else colorScheme.primaryContainer
        val onCard = if (event.isCurrent) colorScheme.onTertiaryContainer else colorScheme.onPrimaryContainer
        // Та же фигура, что у карточки в ленте: звезда события или фигура проекта.
        val shape =
            remember(event.originalEvent.id, event.isProject) {
              if (event.isProject) projectShape(event.originalEvent.id)
              else eventStarShape(event.shapeParams)
            }
        val params = event.shapeParams

        Surface(
            modifier =
                Modifier.fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            shape = RoundedCornerShape(cuid.ContainerCornerRadius),
            color = container,
            tonalElevation = 8.dp) {
              // Высота постоянная: кнопки на одном месте, длинное содержимое листается внутри.
              Box(modifier = Modifier.height(CARD_HEIGHT)) {
                Box(
                    modifier =
                        Modifier.align(Alignment.BottomEnd)
                            // Чуть выглядывает из правого нижнего угла, повернута как в ленте.
                            .graphicsLayer {
                              translationX = SHAPE_SIZE.toPx() * 0.22f
                              translationY = SHAPE_SIZE.toPx() * 0.18f
                              rotationZ = params.rotationAngle
                            }
                            .requiredSize(SHAPE_SIZE)
                            .clip(shape)
                            .background(container)
                            .innerShadow(
                                shape = shape,
                                shadow =
                                    Shadow(
                                        radius = 0.dp,
                                        spread = 0.dp,
                                        color = Color.Black.copy(0.22f),
                                        offset =
                                            DpOffset(params.shadowOffsetXSeed, params.shadowOffsetYSeed))))

                Column(
                    modifier =
                        Modifier.matchParentSize()
                            .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp)) {
                      // Подробности листаются, если не влезают; кнопки остаются внизу.
                      Column(
                          modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            Text(
                                text = event.summary,
                                style = typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = onCard)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = event.formattedTimeString,
                                color = onCard,
                                style = typography.headlineSmall.copy(fontWeight = FontWeight.Normal),
                                maxLines = 2)
                            Spacer(modifier = Modifier.height(16.dp))

                            event.originalEvent.location?.takeIf { it.isNotBlank() }?.let { place ->
                              val context = LocalContext.current
                              // Место — кнопка: открывает карты на этом адресе.
                              DetailRow(
                                  Icons.Filled.LocationOn,
                                  place,
                                  color = onCard,
                                  onClick = { context.openInMaps(place) },
                                  trailing = Icons.AutoMirrored.Rounded.OpenInNew)
                              Spacer(modifier = Modifier.height(12.dp))
                            }
                            event.originalEvent.recurrenceRule?.takeIf { it.isNotEmpty() }?.let { rule ->
                              DetailRow(
                                  Icons.Filled.Repeat,
                                  formatRRule(rule, zoneId = userTimeZone),
                                  color = onCard)
                              Spacer(modifier = Modifier.height(12.dp))
                            }
                            event.originalEvent.description?.takeIf { it.isNotBlank() }?.let { text ->
                              Text(text = text, style = typography.bodyMedium, color = onCard)
                            }
                          }
                      Spacer(Modifier.height(12.dp))
                      Row(
                          modifier = Modifier.fillMaxWidth(),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onEdit(event.originalEvent) },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                                  Icon(Icons.Filled.Edit, contentDescription = null)
                                  Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                  Text(stringResource(R.string.edit))
                                }
                            FilledTonalIconButton(
                                onClick = {
                                  onDelete(event.originalEvent)
                                  // Карточка исчезнет сразу — подробности удалённого не нужны.
                                  onDismissRequest()
                                },
                                modifier = Modifier.size(48.dp)) {
                                  Icon(
                                      imageVector = Icons.Filled.Delete,
                                      contentDescription = stringResource(R.string.delete))
                                }
                          }
                    }
              }
            }
      }
}

private val CARD_HEIGHT = 320.dp
private val SHAPE_SIZE = 280.dp

@Composable
private fun DetailRow(
    icon: ImageVector,
    value: String,
    color: Color,
    onClick: (() -> Unit)? = null,
    trailing: ImageVector? = null,
) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .then(
                  if (onClick != null) {
                    Modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onClick)
                  } else Modifier),
      verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = value, style = typography.bodyLarge, color = color, modifier = Modifier.weight(1f))
        if (trailing != null) {
          Icon(trailing, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
      }
}
