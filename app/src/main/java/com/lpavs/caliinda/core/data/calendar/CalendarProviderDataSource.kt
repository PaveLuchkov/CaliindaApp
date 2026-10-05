package com.lpavs.caliinda.core.data.calendar

import android.accounts.Account
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Instances
import android.util.Log
import com.lpavs.caliinda.core.data.calendar.model.DeviceCalendar
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/** Строка из CalendarContract.Instances в "сыром" виде провайдера. */
data class InstanceRow(
    val eventId: Long,
    val begin: Long,
    val end: Long,
    val title: String?,
    val description: String?,
    val location: String?,
    val isAllDay: Boolean,
    val rrule: String?,
    val originalId: Long?,
    val originalInstanceTime: Long?,
    val calendarId: Long,
    val color: Int?,
)

/** Минимум полей мастер-события, нужный для операций над сериями. */
data class EventRow(
    val id: Long,
    val dtStart: Long,
    val isAllDay: Boolean,
    val rrule: String?,
    val exdate: String?,
    val duration: String?,
    val timeZone: String?,
    /** _SYNC_ID: есть, когда серию уже синхронизировали с сервером (Google). */
    val syncId: String? = null,
)

/**
 * Тонкая обёртка над системным календарём (CalendarContract). Google Calendar синхронизируется в
 * него самой системой, поэтому приложению не нужен ни бэкенд, ни OAuth.
 */
@Singleton
class CalendarProviderDataSource
@Inject
constructor(@ApplicationContext context: Context) {
  private val resolver: ContentResolver = context.contentResolver

  /** Эмитит сразу и затем при каждом изменении данных в провайдере календаря. */
  fun observeChanges(): Flow<Unit> =
      callbackFlow {
            val observer =
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                  override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                  }
                }
            resolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
            trySend(Unit)
            awaitClose { resolver.unregisterContentObserver(observer) }
          }
          .conflate()

  fun queryInstances(beginMillis: Long, endMillis: Long): List<InstanceRow> {
    val uri =
        Instances.CONTENT_URI.buildUpon()
            .also {
              ContentUris.appendId(it, beginMillis)
              ContentUris.appendId(it, endMillis)
            }
            .build()
    val selection =
        "${Instances.VISIBLE} = 1 AND " +
            "(${Instances.STATUS} IS NULL OR ${Instances.STATUS} != ${Events.STATUS_CANCELED})"

    return resolver.query(uri, INSTANCE_PROJECTION, selection, null, "${Instances.BEGIN} ASC")
        ?.use { c ->
          buildList {
            while (c.moveToNext()) {
              add(
                  InstanceRow(
                      eventId = c.getLong(0),
                      begin = c.getLong(1),
                      end = c.getLong(2),
                      title = c.getStringOrNull(3),
                      description = c.getStringOrNull(4),
                      location = c.getStringOrNull(5),
                      isAllDay = c.getInt(6) == 1,
                      rrule = c.getStringOrNull(7)?.takeIf { it.isNotBlank() },
                      originalId = c.getLongOrNull(8),
                      originalInstanceTime = c.getLongOrNull(9),
                      calendarId = c.getLong(10),
                      color = c.getIntOrNull(11)))
            }
          }
        } ?: emptyList()
  }

  fun getEvent(eventId: Long): EventRow? =
      resolver
          .query(
              ContentUris.withAppendedId(Events.CONTENT_URI, eventId),
              arrayOf(
                  Events._ID,
                  Events.DTSTART,
                  Events.ALL_DAY,
                  Events.RRULE,
                  Events.EXDATE,
                  Events.DURATION,
                  Events.EVENT_TIMEZONE,
                  Events._SYNC_ID),
              null,
              null,
              null)
          ?.use { c ->
            if (!c.moveToFirst()) null
            else
                EventRow(
                    id = c.getLong(0),
                    dtStart = c.getLong(1),
                    isAllDay = c.getInt(2) == 1,
                    rrule = c.getStringOrNull(3)?.takeIf { it.isNotBlank() },
                    exdate = c.getStringOrNull(4)?.takeIf { it.isNotBlank() },
                    duration = c.getStringOrNull(5),
                    timeZone = c.getStringOrNull(6),
                    syncId = c.getStringOrNull(7)?.takeIf { it.isNotBlank() })
          }

  fun insertEvent(values: ContentValues): Long {
    val uri = resolver.insert(Events.CONTENT_URI, values) ?: error("Calendar insert failed")
    return ContentUris.parseId(uri)
  }

  /**
   * Исключение из серии [masterId]: провайдер сам заполняет ORIGINAL_ID/ORIGINAL_SYNC_ID и
   * недостающие поля из серии, так что правка остаётся вхождением серии и в Google Calendar.
   * В [values] обязателен ORIGINAL_INSTANCE_TIME.
   */
  fun insertException(masterId: Long, values: ContentValues): Long {
    val uri =
        resolver.insert(ContentUris.withAppendedId(Events.CONTENT_EXCEPTION_URI, masterId), values)
            ?: error("Calendar exception insert failed")
    return ContentUris.parseId(uri)
  }

  /** Удаляет исключения серии с исходным временем от [fromInstanceTime] и позже. */
  fun deleteExceptionsFrom(masterId: Long, fromInstanceTime: Long): Int =
      resolver.delete(
          Events.CONTENT_URI,
          "${Events.ORIGINAL_ID} = ? AND ${Events.ORIGINAL_INSTANCE_TIME} >= ?",
          arrayOf(masterId.toString(), fromInstanceTime.toString()))

  fun updateEvent(eventId: Long, values: ContentValues): Int =
      resolver.update(ContentUris.withAppendedId(Events.CONTENT_URI, eventId), values, null, null)

  fun deleteEvent(eventId: Long): Int =
      resolver.delete(ContentUris.withAppendedId(Events.CONTENT_URI, eventId), null, null)

  fun queryWritableCalendars(): List<DeviceCalendar> {
    val selection =
        "${Calendars.CALENDAR_ACCESS_LEVEL} >= ${Calendars.CAL_ACCESS_CONTRIBUTOR} AND " +
            "${Calendars.VISIBLE} = 1"
    return resolver
        .query(Calendars.CONTENT_URI, CALENDAR_PROJECTION, selection, null, null)
        ?.use { c ->
          buildList {
            while (c.moveToNext()) {
              add(
                  DeviceCalendar(
                      id = c.getLong(0),
                      displayName = c.getStringOrNull(1).orEmpty(),
                      accountName = c.getStringOrNull(2).orEmpty(),
                      accountType = c.getStringOrNull(3).orEmpty(),
                      color = c.getInt(4),
                      isPrimary = c.getInt(5) == 1))
            }
          }
        } ?: emptyList()
  }

  /** Локальный (не синхронизируемый) календарь — на случай, если на устройстве нет аккаунтов. */
  fun createLocalCalendar(): Long {
    val uri =
        Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, LOCAL_ACCOUNT_NAME)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()
    val values =
        ContentValues().apply {
          put(Calendars.ACCOUNT_NAME, LOCAL_ACCOUNT_NAME)
          put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
          put(Calendars.NAME, LOCAL_ACCOUNT_NAME)
          put(Calendars.CALENDAR_DISPLAY_NAME, LOCAL_ACCOUNT_NAME)
          put(Calendars.CALENDAR_COLOR, 0xFF6750A4.toInt())
          put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER)
          put(Calendars.OWNER_ACCOUNT, LOCAL_ACCOUNT_NAME)
          put(Calendars.VISIBLE, 1)
          put(Calendars.SYNC_EVENTS, 1)
          put(Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
        }
    val created = resolver.insert(uri, values) ?: error("Local calendar creation failed")
    return ContentUris.parseId(created)
  }

  /** Просит систему синхронизировать календарные аккаунты (Google и т.п.) прямо сейчас. */
  fun requestSync() {
    val accounts =
        resolver
            .query(
                Calendars.CONTENT_URI,
                arrayOf(Calendars.ACCOUNT_NAME, Calendars.ACCOUNT_TYPE),
                null,
                null,
                null)
            ?.use { c ->
              buildSet {
                while (c.moveToNext()) {
                  val type = c.getStringOrNull(1)
                  val name = c.getStringOrNull(0)
                  if (name != null && type != null && type != CalendarContract.ACCOUNT_TYPE_LOCAL) {
                    add(Account(name, type))
                  }
                }
              }
            } ?: emptySet()

    val extras =
        Bundle().apply {
          putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
          putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
        }
    accounts.forEach { account ->
      try {
        ContentResolver.requestSync(account, CalendarContract.AUTHORITY, extras)
      } catch (e: Exception) {
        Log.w(TAG, "requestSync failed for ${account.type}", e)
      }
    }
  }

  private fun Cursor.getStringOrNull(i: Int): String? = if (isNull(i)) null else getString(i)

  private fun Cursor.getLongOrNull(i: Int): Long? = if (isNull(i)) null else getLong(i)

  private fun Cursor.getIntOrNull(i: Int): Int? = if (isNull(i)) null else getInt(i)

  companion object {
    private const val TAG = "CalendarProvider"
    const val LOCAL_ACCOUNT_NAME = "Caliinda"

    private val INSTANCE_PROJECTION =
        arrayOf(
            Instances.EVENT_ID,
            Instances.BEGIN,
            Instances.END,
            Instances.TITLE,
            Instances.DESCRIPTION,
            Instances.EVENT_LOCATION,
            Instances.ALL_DAY,
            Instances.RRULE,
            Instances.ORIGINAL_ID,
            Instances.ORIGINAL_INSTANCE_TIME,
            Instances.CALENDAR_ID,
            Instances.DISPLAY_COLOR)

    private val CALENDAR_PROJECTION =
        arrayOf(
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.ACCOUNT_NAME,
            Calendars.ACCOUNT_TYPE,
            Calendars.CALENDAR_COLOR,
            Calendars.IS_PRIMARY)
  }
}
