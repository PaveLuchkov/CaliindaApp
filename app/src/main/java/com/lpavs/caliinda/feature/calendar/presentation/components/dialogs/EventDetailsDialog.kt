package com.lpavs.caliinda.feature.calendar.presentation.components.dialogs

import java.time.ZoneId
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lpavs.caliinda.core.ui.util.formatRRule
import com.lpavs.caliinda.feature.calendar.presentation.model.EventDetailsUiModel
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.model.ThemeMode

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomEventDetailsDialog(
    event: EventDetailsUiModel,
    userTimeZone: ZoneId,
    onDismissRequest: () -> Unit,
    onEdit: (EventDto) -> Unit,
    onDelete: (EventDto) -> Unit,
    themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    val modeShape: Shape = when(themeMode) {
        ThemeMode.SYSTEM -> MaterialShapes.Cookie7Sided.toShape()
        ThemeMode.SUNNY -> MaterialShapes.Sunny.toShape()
        ThemeMode.WARM -> MaterialShapes.VerySunny.toShape() // Проверь названия в своих шейпах
        ThemeMode.GREEN -> MaterialShapes.Arrow.toShape()
        ThemeMode.COLD -> MaterialShapes.Burst.toShape()
        ThemeMode.PINKY -> MaterialShapes.Heart.toShape()
    }
  Dialog(
      onDismissRequest = onDismissRequest,
      properties =
          DialogProperties(
              dismissOnBackPress = true,
              dismissOnClickOutside = true,
              usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
            shape = RoundedCornerShape(cuid.ContainerCornerRadius),
            color =
                if (!event.isCurrent) colorScheme.primaryContainer
                else colorScheme.tertiaryContainer,
            tonalElevation = 8.dp) {
              val onCardText =
                  if (!event.isCurrent) colorScheme.onPrimaryContainer
                  else colorScheme.onTertiaryContainer
              Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier =
                        Modifier.align(Alignment.BottomEnd)
                            .size(250.dp)
                            .rotate(-25f)
                            .offset(y = (-50).dp, x = 50.dp)
                            .clip(modeShape)
                            .border(
                                width = 2.dp,
                                color = onCardText.copy(alpha = 0.2f),
                                shape = modeShape)
                            .background(onCardText.copy(alpha = 0f))) {}

                Column(
                    modifier =
                        Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.Start) {
                      Text(
                          text = event.summary,
                          style = typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
                          color = onCardText)
                      Spacer(modifier = Modifier.height(2.dp))
                      Row {
                        Text(
                            text = event.formattedTimeString,
                            color = onCardText,
                            style = typography.headlineSmall.copy(fontWeight = FontWeight.Normal),
                            maxLines = 2)
                      }
                      Spacer(modifier = Modifier.height(16.dp))

                      if (!event.originalEvent.description.isNullOrBlank()) {
                        Text(
                            text = event.originalEvent.description,
                            style = typography.bodyMedium,
                            color = onCardText)
                        Spacer(modifier = Modifier.height(16.dp))
                      }

                      if (!event.originalEvent.location.isNullOrBlank()) {
                        DetailRow(
                            Icons.Filled.LocationOn,
                            event.originalEvent.location,
                            color = onCardText)
                        Spacer(modifier = Modifier.height(16.dp))
                      }

                      if (!event.originalEvent.recurrenceRule.isNullOrEmpty()) {
                        DetailRow(
                            Icons.Filled.Repeat,
                            formatRRule(
                                event.originalEvent.recurrenceRule, zoneId = userTimeZone),
                            color = onCardText)
                      }
                      Spacer(modifier = Modifier.height(20.dp))
                      Row(
                          modifier = Modifier.fillMaxWidth(),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = {
                                  onEdit(event.originalEvent)
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp)) {
                                  Icon(Icons.Filled.Edit, contentDescription = null)
                                  Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                  Text(stringResource(R.string.edit))
                                }
                            //                    Spacer(modifier = Modifier.width(4.dp))
                            FilledIconButton(
                                onClick = {
                                  onDelete(event.originalEvent)
                                  // Карточка исчезнет сразу — подробности удалённого не нужны.
                                  onDismissRequest()
                                },
                                modifier =
                                    Modifier.minimumInteractiveComponentSize()
                                        .size(
                                            IconButtonDefaults.smallContainerSize(
                                                IconButtonDefaults.IconButtonWidthOption.Narrow)),
                                shape = IconButtonDefaults.smallRoundShape) {
                                  Icon(
                                      imageVector = Icons.Filled.Delete,
                                      contentDescription = stringResource(R.string.delete),
                                  )
                                }
                          }
                    }
              }
            }
      }
}

@Composable
private fun DetailRow(icon: ImageVector, value: String, color: Color) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Icon(imageVector = icon, contentDescription = null)
    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
    Text(text = value, style = typography.bodyLarge, color = color)
    Spacer(modifier = Modifier.height(8.dp))
  }
}
