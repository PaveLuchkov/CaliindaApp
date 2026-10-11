package com.lpavs.caliinda.feature.event_management.ui.create

import com.lpavs.caliinda.feature.event_management.ui.shared.sections.repeatOn
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.calendar.model.DeviceCalendar
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.CalendarField
import androidx.compose.runtime.saveable.rememberSaveable
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.ReminderDefaults
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.ReminderChoice
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.feature.event_management.ui.shared.EventFormContent
import com.lpavs.caliinda.feature.event_management.ui.shared.SaveBar
import androidx.compose.ui.focus.FocusRequester
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventDateTimeState
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.suggestions.SuggestionsViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreateEventScreen(
    userTimeZone: ZoneId,
    isLoading: Boolean,
    onSave: OnCreateEvent,
    reminderDefaults: ReminderDefaults,
    /** Календари, куда можно сохранить, и календарь по умолчанию из настроек. */
    calendars: List<DeviceCalendar>,
    defaultCalendarId: Long?,
    initialDate: LocalDate,
    initialProject: Boolean = false,
    /** «Ещё раз»: заполнить форму как у этого события, сдвинув его на [initialDate]. */
    template: EventDto? = null,
    /** Напоминания [template]; null — нет шаблона или ещё загружаются. */
    templateReminders: List<Int>? = null,
    /** Шторка встала на место — можно звать клавиатуру, не сбивая анимацию появления. */
    sheetSettled: Boolean = true,
    suggestionsViewModel: SuggestionsViewModel = hiltViewModel(),
) {
  // Состояние полей
  // «(Без названия)» шаблона в поле не подставляем — сохранилось бы как настоящее название.
  var summary by remember { mutableStateOf(template?.takeUnless { it.isUntitled }?.summary ?: "") }
  var description by remember { mutableStateOf(template?.description ?: "") }
  var location by remember { mutableStateOf(template?.location ?: "") }

  // Ошибки
  var summaryError by remember { mutableStateOf<String?>(null) }


  // Начальное состояние даты и времени
  var eventDateTimeState by remember {
    if (template != null) return@remember mutableStateOf(template.repeatOn(initialDate, userTimeZone))
    if (initialProject) {
      // Проект: весь день, сразу промежуток из нескольких дней.
      return@remember mutableStateOf(
          EventDateTimeState(
              startDate = initialDate,
              startTime = null,
              endDate = initialDate.plusDays(1),
              endTime = null,
              isAllDay = true,
              isRecurring = false))
    }
    // Начало — следующий ровный час. Для сегодняшнего дня считаем с датой: в 23:30 это уже
    // 00:00 завтрашнего дня, а не прошедшая полночь сегодняшнего.
    val now = LocalDateTime.now(userTimeZone)
    val nextHour = now.plusHours(1).truncatedTo(ChronoUnit.HOURS)
    val defaultStart =
        if (initialDate == now.toLocalDate()) nextHour
        else initialDate.atTime(nextHour.toLocalTime())
    val defaultEnd = defaultStart.plusHours(1)

    mutableStateOf(
        EventDateTimeState(
            startDate = defaultStart.toLocalDate(),
            startTime = defaultStart.toLocalTime(),
            endDate = defaultEnd.toLocalDate(),
            endTime = defaultEnd.toLocalTime(),
            isAllDay = false,
            isRecurring = false))
  }

  // Обновление контекста для чипсов-подсказок
  LaunchedEffect(eventDateTimeState.startTime, eventDateTimeState.isAllDay) {
    suggestionsViewModel.updateSortContext(
        eventDateTimeState.startTime, eventDateTimeState.isAllDay)
  }
  val suggestedChips by suggestionsViewModel.suggestionChips.collectAsStateWithLifecycle()

  // Пока не выбрали своё — напоминание из настроек (своё для «весь день» и со временем).
  var reminder by remember { mutableStateOf<ReminderChoice>(ReminderChoice.Default) }
  LaunchedEffect(templateReminders) {
    if (templateReminders != null && reminder == ReminderChoice.Default) {
      reminder = ReminderChoice.Custom(templateReminders.firstOrNull())
    }
  }
  // null — пока не выбрали: следуем календарю по умолчанию, даже если он загрузится позже.
  var chosenCalendarId by rememberSaveable { mutableStateOf<Long?>(null) }
  val calendar =
      calendars.firstOrNull { it.id == chosenCalendarId }
          ?: calendars.firstOrNull { it.id == defaultCalendarId }
          ?: calendars.firstOrNull()
  val save = {
    onSave(summary, description, location, eventDateTimeState, reminder, calendar?.id)
  }
  // Сразу клавиатура в названии: типичное событие — это название и время. У копии название уже
  // есть, менять скорее будут день и время — клавиатура закрыла бы их.
  val nameFocusRequester = remember { FocusRequester() }
  LaunchedEffect(sheetSettled) {
    if (sheetSettled && template == null) runCatching { nameFocusRequester.requestFocus() }
  }

  // Шторка по содержимому: кнопка сразу под формой, без пустоты до низа экрана. Если форма
  // не влезает (клавиатура), она прокручивается, а кнопка остаётся видна.
  Column(modifier = Modifier.fillMaxWidth()) {
    EventFormContent(
        summary = summary,
        onSummaryChange = { summary = it },
        summaryError = summaryError,
        onSummaryErrorChange = { summaryError = it },
        description = description,
        onDescriptionChange = { description = it },
        location = location,
        onLocationChange = { location = it },
        dateTimeState = eventDateTimeState,
        onDateTimeStateChange = { eventDateTimeState = it },
        zone = userTimeZone,
        reminder = reminder,
        onReminderChange = { reminder = it },
        reminderDefaults = reminderDefaults,
        isLoading = isLoading,
        suggestedChips = suggestedChips,
        onSave = save,
        nameFocusRequester = nameFocusRequester,
        calendarField =
            calendar?.let { selected ->
              {
                CalendarField(
                    calendars = calendars,
                    selected = selected,
                    onSelect = { chosenCalendarId = it },
                    enabled = !isLoading)
              }
            },
        modifier = Modifier.weight(1f, fill = false))
    SaveBar(enabled = summary.isNotBlank(), isLoading = isLoading, onSave = save)
  }
}

/** Сохранение из формы создания: как [OnSaveEvent], плюс выбранный календарь. */
typealias OnCreateEvent =
    (
        summary: String,
        description: String,
        location: String,
        dateTime: EventDateTimeState,
        reminder: ReminderChoice,
        calendarId: Long?,
    ) -> Unit
