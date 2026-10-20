package com.lpavs.caliinda.feature.calendar.presentation.components.dialogs

import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.eventStarShape
import com.lpavs.caliinda.core.ui.util.projectShape
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.ui.platform.LocalContext
import com.lpavs.caliinda.core.ui.util.openPlace
import com.lpavs.caliinda.core.ui.util.placeIcon
import com.lpavs.caliinda.core.ui.util.placeLabel
import com.lpavs.caliinda.core.data.utils.eventPlace
import java.time.ZoneId
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import com.lpavs.caliinda.core.ui.theme.AppMotion
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.animation.animateContentSize
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import com.lpavs.caliinda.core.data.calendar.model.AttendeeStatus
import com.lpavs.caliinda.core.data.calendar.model.Attendee
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.Group
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.lpavs.caliinda.core.ui.util.EventEnds
import java.time.LocalDate
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
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lpavs.caliinda.core.ui.util.formatRRule
import com.lpavs.caliinda.feature.calendar.presentation.model.EventDetailsUiModel
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.calendar.model.DeviceCalendar
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomEventDetailsDialog(
    event: EventDetailsUiModel,
    userTimeZone: ZoneId,
    onDismissRequest: () -> Unit,
    onEdit: (EventDto) -> Unit,
    onDelete: (EventDto) -> Unit,
    onRepeat: (EventDto) -> Unit,
    /** Тап по дате начала или конца многодневного события — перейти на этот день. */
    onOpenDate: (LocalDate) -> Unit,
    /** Календарь события; null — не показываем (он один или не найден). */
    calendar: DeviceCalendar? = null,
) {
  Dialog(
      onDismissRequest = onDismissRequest,
      properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val container = if (event.isCurrent) colorScheme.tertiaryContainer else colorScheme.primaryContainer
        val onCard = if (event.isCurrent) colorScheme.onTertiaryContainer else colorScheme.onPrimaryContainer
        // Та же фигура, что у карточки в ленте: звезда события или фигура проекта.
        val shape =
            remember(event.originalEvent.id, event.isProject) {
              if (event.isProject) projectShape(event.originalEvent.id)
              else eventStarShape(event.shapeParams)
            }
        val params = event.shapeParams

        // Окно — на весь экран, карточка — по центру внутри. Окно по размеру карточки менялось
        // бы на каждом кадре её роста: система перекраивает и перецентрирует его — рывки.
        // Тап мимо карточки закрывает, как у обычного диалога.
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .clickable(interactionSource = null, indication = null, onClick = onDismissRequest),
            contentAlignment = Alignment.Center) {
        Surface(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    // Тапы по самой карточке не проваливаются в подложку и не закрывают её.
                    .clickable(interactionSource = null, indication = null) {},
            shape = RoundedCornerShape(cuid.ContainerCornerRadius),
            color = container,
            tonalElevation = 8.dp) {
              // Высота — по содержимому: пустая карточка не выше нужного, с местом, участниками и
              // описанием — растёт (плавно, когда участники догрузились или раскрылись). Выше
              // ~80% экрана — листается внутри, кнопки остаются внизу.
              val maxHeight = LocalConfiguration.current.screenHeightDp.dp * MAX_HEIGHT_FRACTION
              Box(
                  modifier =
                      Modifier.heightIn(min = MIN_CARD_HEIGHT, max = maxHeight)
                          // Без пружины: отскок на высоте читается как тяжёлое сползание.
                          .animateContentSize(tween(SIZE_ANIMATION_MS, easing = FastOutSlowInEasing))) {
                Box(
                    modifier =
                        Modifier.matchParentSize()
                            // Фигура больше низкой карточки: держим её у правого нижнего угла, а
                            // не по центру, — без unbounded её бы сжало и сдвинуло.
                            .wrapContentSize(Alignment.BottomEnd, unbounded = true)
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
                    modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp)) {
                      // Подробности листаются, только если не влезли в предел; кнопки — внизу.
                      Column(
                          modifier =
                              Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                            Text(
                                text = event.summary,
                                style = typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = onCard)
                            Spacer(modifier = Modifier.height(2.dp))
                            val ends = event.ends
                            if (ends == null) {
                              Text(
                                  text = event.formattedTimeString,
                                  color = onCard,
                                  style = typography.headlineSmall.copy(fontWeight = FontWeight.Normal),
                                  maxLines = 2)
                            } else {
                              EndsRow(ends, color = onCard, onOpenDate = onOpenDate)
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            val place =
                                remember(event.originalEvent) {
                                  eventPlace(event.originalEvent.location, event.originalEvent.description)
                                }
                            place?.let {
                              val context = LocalContext.current
                              // Место — кнопка: адрес открывает карты, ссылка — Zoom/Meet/браузер.
                              DetailRow(
                                  placeIcon(it),
                                  placeLabel(it),
                                  color = onCard,
                                  onClick = { context.openPlace(it) },
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
                            if (event.attendees.isNotEmpty()) {
                              AttendeesSection(event.attendees, color = onCard)
                              Spacer(modifier = Modifier.height(12.dp))
                            }
                            calendar?.let {
                              CalendarRow(it, color = onCard)
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
                            // Того же цвета, что «Изменить»: действия одной группы.
                            FilledIconButton(
                                onClick = { onRepeat(event.originalEvent) },
                                modifier = Modifier.size(48.dp)) {
                                  Icon(
                                      imageVector = Icons.Filled.Replay,
                                      contentDescription = stringResource(R.string.repeat_event))
                                }
                            FilledIconButton(
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
}

/** Карточка без деталей: название, время и кнопки — не выше этого. */
private val MIN_CARD_HEIGHT = 200.dp
private const val MAX_HEIGHT_FRACTION = 0.8f
private const val SIZE_ANIMATION_MS = 250
/** Сколько ждать раскрытия списка участников, прежде чем подкрутить к нему. */
private const val LIST_REVEAL_MS = 250L
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

/** Календарь события: точка его цвета на месте иконки (DESIGN.md), название и аккаунт. */
@Composable
private fun CalendarRow(calendar: DeviceCalendar, color: Color) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
      Box(Modifier.size(14.dp).clip(CircleShape).background(Color(calendar.color)))
    }
    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
    Text(
        text = calendar.displayName,
        style = typography.bodyLarge,
        color = color,
        maxLines = 1,
        modifier = Modifier.weight(1f, fill = false))
    // Основной календарь Google назван почтой — второй раз её не повторяем.
    if (calendar.accountName != calendar.displayName) {
      Text(
          text = " · ${calendar.accountName}",
          style = typography.bodyMedium,
          color = color.copy(alpha = 0.7f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false))
    }
  }
}

/**
 * Начало и конец многодневного события — две кнопки: видно, что это разные дни, и по тапу
 * можно на них перейти. Подложка лёгкая, цветом текста карточки — без нового цвета в карточке.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EndsRow(ends: EventEnds, color: Color, onOpenDate: (LocalDate) -> Unit) {
  FlowRow(
      verticalArrangement = Arrangement.spacedBy(6.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      itemVerticalAlignment = Alignment.CenterVertically) {
        DateChip(ends.startText, color) { onOpenDate(ends.startDate) }
        Text("–", style = typography.titleLarge, color = color)
        DateChip(ends.endText, color) { onOpenDate(ends.endDate) }
      }
}

@Composable
private fun DateChip(text: String, color: Color, onClick: () -> Unit) {
  Text(
      text = text,
      style = typography.titleLarge.copy(fontWeight = FontWeight.Normal),
      color = color,
      maxLines = 1,
      modifier =
          Modifier.clip(MaterialTheme.shapes.medium)
              .background(color.copy(alpha = 0.1f))
              .clickable(role = Role.Button, onClick = onClick)
              .padding(horizontal = 10.dp, vertical = 4.dp))
}

/**
 * Участники: свёрнуто — «Олег, Аня и ещё 3 · 3 из 6 придут», по тапу — список с ответами.
 * Цвета — только цветом текста карточки с разной прозрачностью, как остальная карточка.
 */
@Composable
private fun AttendeesSection(attendees: List<Attendee>, color: Color) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  val others = attendees.filterNot { it.isSelf }
  val shownNames = others.take(2).joinToString(", ") { it.name }
  val names =
      if (others.size > 2) stringResource(R.string.attendees_and_more, shownNames, others.size - 2)
      else shownNames
  val going = attendees.count { it.status == AttendeeStatus.ACCEPTED }
  Column {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.Button) { expanded = !expanded },
        verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.Group, contentDescription = null, tint = color)
          Spacer(Modifier.size(ButtonDefaults.IconSpacing))
          Column(Modifier.weight(1f)) {
            Text(names, style = typography.bodyLarge, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.attendees_going, going, attendees.size),
                style = typography.bodySmall,
                color = color.copy(alpha = 0.7f))
          }
          Icon(
              if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
              contentDescription = stringResource(R.string.attendees_toggle),
              tint = color)
        }
    // Карточка фиксированной высоты: раскрытый список оказался бы ниже края — подкручиваем к нему.
    val listInView = remember { BringIntoViewRequester() }
    LaunchedEffect(expanded) {
      if (expanded) {
        delay(LIST_REVEAL_MS)
        listInView.bringIntoView()
      }
    }
    AnimatedVisibility(visible = expanded) {
      Column(
          modifier = Modifier.padding(top = 8.dp).bringIntoViewRequester(listInView),
          verticalArrangement = Arrangement.spacedBy(8.dp)) {
            attendees.forEach { AttendeeRow(it, color) }
          }
    }
  }
}

@Composable
private fun AttendeeRow(attendee: Attendee, color: Color) {
  // Не придёт — приглушаем: человек в списке есть, но на встрече его не будет.
  val tone = if (attendee.status == AttendeeStatus.DECLINED) color.copy(alpha = 0.55f) else color
  // Себя — «Вы», а не своей почтой (и в кружке тоже); роль в подписи — только «организатор».
  val name =
      if (attendee.isSelf) stringResource(R.string.attendee_you).replaceFirstChar { it.titlecase() }
      else attendee.name
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.12f))) {
          Text(
              name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?",
              style = typography.labelLarge,
              color = tone)
        }
    Spacer(Modifier.size(12.dp))
    Column(Modifier.weight(1f)) {
      Text(
          if (attendee.isOrganizer) "$name · ${stringResource(R.string.attendee_organizer)}" else name,
          style = typography.bodyMedium,
          color = tone,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis)
      Text(
          stringResource(attendee.status.labelRes()),
          style = typography.bodySmall,
          color = tone.copy(alpha = tone.alpha * 0.75f))
    }
    Icon(attendee.status.icon(), contentDescription = null, tint = tone, modifier = Modifier.size(18.dp))
  }
}

private fun AttendeeStatus.labelRes(): Int =
    when (this) {
      AttendeeStatus.ACCEPTED -> R.string.attendee_accepted
      AttendeeStatus.TENTATIVE -> R.string.attendee_tentative
      AttendeeStatus.AWAITING -> R.string.attendee_awaiting
      AttendeeStatus.DECLINED -> R.string.attendee_declined
    }

private fun AttendeeStatus.icon(): ImageVector =
    when (this) {
      AttendeeStatus.ACCEPTED -> Icons.Rounded.CheckCircle
      AttendeeStatus.TENTATIVE -> Icons.Rounded.Help
      AttendeeStatus.AWAITING -> Icons.Rounded.Schedule
      AttendeeStatus.DECLINED -> Icons.Rounded.Cancel
    }
