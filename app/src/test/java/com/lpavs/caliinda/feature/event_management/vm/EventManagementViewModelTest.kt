package com.lpavs.caliinda.feature.event_management.vm

import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.calendar.model.EventUpdateMode
import com.lpavs.caliinda.core.data.repository.CalendarRepository
import com.lpavs.caliinda.core.data.repository.PendingDeletions
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import com.lpavs.caliinda.core.data.utils.UiText
import com.lpavs.caliinda.feature.event_management.messages.IFunMessages
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.toDateTimeState
import com.lpavs.caliinda.feature.widget.WidgetRefresher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class EventManagementViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val zone = ZoneId.of("Europe/Moscow")
  private val repository: CalendarRepository = mock()
  private val message = UiText.DynamicString("ok")
  private val funMessages: IFunMessages = mock {
    on { getEventUpdatedMessage(any()) } doReturn message
    on { getEventCreatedMessage(any()) } doReturn message
  }
  private lateinit var viewModel: EventManagementViewModel

  private val single =
      EventDto(
          id = "1_1",
          summary = "Встреча",
          startTime = Instant.parse("2026-10-05T07:00:00Z"),
          endTime = Instant.parse("2026-10-05T08:00:00Z"))
  private val recurring = single.copy(recurringEventId = "1", recurrenceRule = "FREQ=DAILY")

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    val settings: SettingsRepository = mock { on { zoneFlow } doReturn flowOf(zone) }
    viewModel =
        EventManagementViewModel(
            settings, repository, funMessages, PendingDeletions(), mock<WidgetRefresher>())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `обычное событие сразу открывается на правку`() {
    viewModel.requestEditEvent(single)
    assertEquals(
        EventDialog.Editing(single, EventUpdateMode.ALL_IN_SERIES), viewModel.uiState.value.dialog)
  }

  @Test
  fun `у повторяющегося сначала выбор режима, потом правка`() {
    viewModel.requestEditEvent(recurring)
    assertEquals(EventDialog.ChooseEditMode(recurring), viewModel.uiState.value.dialog)

    viewModel.onRecurringEditOptionSelected(EventUpdateMode.SINGLE_INSTANCE)
    assertEquals(
        EventDialog.Editing(recurring, EventUpdateMode.SINGLE_INSTANCE),
        viewModel.uiState.value.dialog)
  }

  @Test
  fun `удаление повторяющегося спрашивает режим`() {
    viewModel.requestDelete(recurring)
    assertEquals(EventDialog.ChooseDeleteMode(recurring), viewModel.uiState.value.dialog)
    viewModel.dismissDialog()
    assertEquals(EventDialog.None, viewModel.uiState.value.dialog)
  }

  @Test
  fun `после успешного сохранения шторка закрывается и приходит сообщение`() = runTest(dispatcher) {
    whenever(repository.updateEvent(any(), any(), any())).thenReturn(Result.success(Unit))
    advanceUntilIdle() // пояс из настроек подтянулся
    viewModel.requestEditEvent(single)

    viewModel.updateEvent(
        "Новое название", "", "", single.toDateTimeState(zone), EventUpdateMode.ALL_IN_SERIES)
    advanceUntilIdle()

    assertEquals(EventDialog.None, viewModel.uiState.value.dialog)
    assertEquals(EventManagementUiEvent.ShowMessage(message), viewModel.events.first())
  }

  @Test
  fun `без изменений — не пишем в календарь, но шторку закрываем`() = runTest(dispatcher) {
    advanceUntilIdle()
    viewModel.requestEditEvent(single)

    viewModel.updateEvent(
        single.summary, "", "", single.toDateTimeState(zone), EventUpdateMode.ALL_IN_SERIES)
    advanceUntilIdle()

    assertEquals(EventDialog.None, viewModel.uiState.value.dialog)
    assertTrue(viewModel.events.first() is EventManagementUiEvent.ShowMessage)
  }

  @Test
  fun `создание открывает шторку с нужной датой`() {
    val date = LocalDate.of(2026, 10, 7)
    viewModel.openCreate(date, asProject = true)
    assertEquals(EventDialog.Creating(date, asProject = true), viewModel.uiState.value.dialog)
  }
}
