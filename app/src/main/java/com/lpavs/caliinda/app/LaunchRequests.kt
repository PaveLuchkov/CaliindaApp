package com.lpavs.caliinda.app

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Просьбы к главному экрану снаружи — от виджетов и других приложений (стандартные календарные
 * интенты). Флаг, а не событие: просьба должна дожить до момента, когда экран календаря
 * соберётся (холодный старт, возврат из настроек).
 */
object LaunchRequests {
  const val ACTION_CREATE_EVENT = "com.lpavs.caliinda.action.CREATE_EVENT"
  const val ACTION_OPEN_EVENT = "com.lpavs.caliinda.action.OPEN_EVENT"
  const val EXTRA_EVENT_ID = "event_id"
  const val EXTRA_INSTANCE_BEGIN = "instance_begin"
  const val EXTRA_EPOCH_DAY = "epoch_day"

  private val _createEvent = MutableStateFlow(false)
  val createEvent: StateFlow<Boolean> = _createEvent.asStateFlow()

  private val _openEvent = MutableStateFlow<OpenEventRequest?>(null)
  /** Тап по событию на виджете или ссылка на событие из другого приложения. */
  val openEvent: StateFlow<OpenEventRequest?> = _openEvent.asStateFlow()

  private val _insertEvent = MutableStateFlow<InsertEventRequest?>(null)
  /** «Добавить в календарь» из другого приложения или .ics-файл: форма с готовыми полями. */
  val insertEvent: StateFlow<InsertEventRequest?> = _insertEvent.asStateFlow()

  private val _openTime = MutableStateFlow<Long?>(null)
  /** «Открыть календарь на этом времени» (content://com.android.calendar/time/<ms>). */
  val openTime: StateFlow<Long?> = _openTime.asStateFlow()

  fun handle(intent: Intent?) {
    when (intent?.action) {
      ACTION_CREATE_EVENT -> _createEvent.value = true
      ACTION_OPEN_EVENT ->
          _openEvent.value =
              OpenEventRequest(
                  eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1),
                  instanceBegin = intent.getLongExtra(EXTRA_INSTANCE_BEGIN, -1),
                  date = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_EPOCH_DAY, 0)))
      Intent.ACTION_INSERT -> _insertEvent.value = intent.toInsertRequest()
      // EDIT без id в ссылке — старый способ «добавить событие» (до ICS), так зовут до сих пор.
      Intent.ACTION_EDIT ->
          intent.data?.eventId()?.let { _openEvent.value = intent.toOpenRequest(it, edit = true) }
              ?: run { _insertEvent.value = intent.toInsertRequest() }
      Intent.ACTION_VIEW -> {
        val uri = intent.data ?: return
        uri.eventId()?.let { _openEvent.value = intent.toOpenRequest(it, edit = false) }
        uri.timeMillis()?.let { _openTime.value = it }
      }
    }
  }

  /** Событие из .ics-файла: файл читается вне главного потока, потом — как обычная вставка. */
  fun requestInsert(request: InsertEventRequest) {
    _insertEvent.value = request
  }

  fun consumeOpenEvent() {
    _openEvent.value = null
  }

  fun consumeCreateEvent() {
    _createEvent.value = false
  }

  fun consumeInsertEvent() {
    _insertEvent.value = null
  }

  fun consumeOpenTime() {
    _openTime.value = null
  }

  private fun Intent.toInsertRequest() =
      InsertEventRequest(
          title = getStringExtra(Events.TITLE).orEmpty(),
          description = getStringExtra(Events.DESCRIPTION).orEmpty(),
          location = getStringExtra(Events.EVENT_LOCATION).orEmpty(),
          beginMillis = longExtraOrNull(CalendarContract.EXTRA_EVENT_BEGIN_TIME),
          endMillis = longExtraOrNull(CalendarContract.EXTRA_EVENT_END_TIME),
          isAllDay = getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false))

  // Начало экземпляра серии приложения кладут в extra; без него откроем само событие.
  private fun Intent.toOpenRequest(eventId: Long, edit: Boolean) =
      OpenEventRequest(
          eventId = eventId,
          instanceBegin = longExtraOrNull(CalendarContract.EXTRA_EVENT_BEGIN_TIME) ?: -1,
          date = null,
          edit = edit)

  // Кто-то кладёт время Int'ом, кто-то строкой — берём, что разберётся.
  @Suppress("DEPRECATION")
  private fun Intent.longExtraOrNull(key: String): Long? =
      when (val value = extras?.get(key)) {
        is Long -> value
        is Int -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
      }

  private fun Uri.isCalendar() = scheme == "content" && authority == CalendarContract.AUTHORITY

  /** content://com.android.calendar/events/<id> */
  private fun Uri.eventId(): Long? =
      if (isCalendar() && pathSegments.firstOrNull() == "events")
          runCatching { ContentUris.parseId(this) }.getOrNull()?.takeIf { it >= 0 }
      else null

  /** content://com.android.calendar/time/<ms> */
  private fun Uri.timeMillis(): Long? =
      if (isCalendar() && pathSegments.firstOrNull() == "time")
          pathSegments.getOrNull(1)?.toLongOrNull()
      else null
}

/**
 * Какое событие открыть: экземпляр серии — по началу ([instanceBegin], -1 — не знаем); [date] —
 * день, который показать (null — найти по событию). [edit] — сразу правка, а не подробности.
 */
data class OpenEventRequest(
    val eventId: Long,
    val instanceBegin: Long,
    val date: LocalDate?,
    val edit: Boolean = false,
)

/**
 * Новое событие с полями от другого приложения. Время — как в CalendarContract: миллисекунды,
 * у all-day — полночь UTC; null — не передали, форма предложит своё.
 */
data class InsertEventRequest(
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val beginMillis: Long? = null,
    val endMillis: Long? = null,
    val isAllDay: Boolean = false,
)
