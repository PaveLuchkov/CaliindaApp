package com.lpavs.caliinda.feature.event_management.ui.details

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.core.ui.util.DateTimeFormatterUtil
import com.lpavs.caliinda.core.ui.util.DateTimeFormatterUtil.formatRRule
import com.lpavs.caliinda.core.ui.util.DateTimeUtils.parseToInstant
import com.lpavs.caliinda.feature.calendar.ui.CalendarViewModel
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomEventDetailsDialog(
    event: EventDto,
    userTimeZone: String,
    onDismissRequest: () -> Unit,
    viewModel: CalendarViewModel,
    eventManagementViewModel: EventManagementViewModel
) {
  val context = LocalContext.current
  val currentLocale = LocalConfiguration.current.getLocales().get(0)
  val timeFormatterLambda: (EventDto) -> String =
      remember(viewModel, userTimeZone, currentLocale) {
        { event ->
          DateTimeFormatterUtil.formatEventDetailsTime(
              context, event, userTimeZone, currentLocale)
        }
      }
  val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
  val isCurrent =
      remember(currentTime, event.startTime, event.endTime) {
        val start = parseToInstant(event.startTime, userTimeZone)
        val end = parseToInstant(event.endTime, userTimeZone)
        start != null && end != null && !currentTime.isBefore(start) && currentTime.isBefore(end)
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
            shape = RoundedCornerShape(25.dp),
            color = if (!isCurrent) colorScheme.primaryContainer else colorScheme.tertiaryContainer,
            tonalElevation = 8.dp) {
              val onCardText =
                  if (!isCurrent) colorScheme.onPrimaryContainer
                  else colorScheme.onTertiaryContainer
              Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier =
                        Modifier.align(Alignment.BottomEnd)
                            .size(250.dp)
                            .rotate(75f)
                            .offset(y = (-50).dp, x = 50.dp)
                            .clip(MaterialShapes.Cookie7Sided.toShape())
                            .border(
                                width = 2.dp,
                                color = onCardText.copy(alpha = 0.2f),
                                shape = MaterialShapes.Cookie7Sided.toShape())
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
                            text = timeFormatterLambda(event),
                            color = onCardText,
                            style = typography.headlineSmall.copy(fontWeight = FontWeight.Normal),
                            maxLines = 2)
                      }
                      Spacer(modifier = Modifier.height(16.dp))

                      if (!event.description.isNullOrBlank()) {
                        Text(
                            text = event.description,
                            style = typography.bodyMedium,
                            color = onCardText)
                        Spacer(modifier = Modifier.height(16.dp))
                      }

                      if (!event.location.isNullOrBlank()) {
                        DetailRow(Icons.Filled.LocationOn, event.location, color = onCardText)
                        Spacer(modifier = Modifier.height(16.dp))
                      }

                      if (!event.recurrenceRule.isNullOrEmpty()) {
                        DetailRow(
                            Icons.Filled.Repeat,
                            formatRRule(event.recurrenceRule, zoneIdString = userTimeZone),
                            color = onCardText)
                      }
                      Spacer(modifier = Modifier.height(20.dp))
                      Row(
                          modifier = Modifier.fillMaxWidth(),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = { eventManagementViewModel.requestEditEvent(event) },
                                contentPadding = PaddingValues(horizontal = 12.dp)) {
                                  Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                  Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                  Text("Edit") // Или локализованная строка
                                }
                            //                    Spacer(modifier = Modifier.width(4.dp))
                            FilledIconButton(
                                onClick = {
                                  eventManagementViewModel.requestDeleteConfirmation(event)
                                },
                                modifier =
                                    Modifier.minimumInteractiveComponentSize()
                                        .size(
                                            IconButtonDefaults.smallContainerSize(
                                                IconButtonDefaults.IconButtonWidthOption.Narrow)),
                                shape = IconButtonDefaults.smallRoundShape) {
                                  Icon(
                                      imageVector = Icons.Filled.Delete,
                                      contentDescription = "Delete",
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
    Icon(imageVector = icon, contentDescription = "Описание иконки")
    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
    Text(text = value, style = typography.bodyLarge, color = color)
    Spacer(modifier = Modifier.height(8.dp))
  }
}
