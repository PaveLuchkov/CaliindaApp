package com.lpavs.caliinda.core.data.repository

import android.util.Log
import com.lpavs.caliinda.app.di.IoDispatcher
import com.lpavs.caliinda.core.common.EventNetworkState
import com.lpavs.caliinda.core.data.auth.AuthManager
import com.lpavs.caliinda.core.data.local.CalendarLocalDataSource
import com.lpavs.caliinda.core.data.remote.CalendarRemoteDataSource
import com.lpavs.caliinda.core.data.remote.EventDeleteMode
import com.lpavs.caliinda.core.data.remote.EventUpdateMode
import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.core.data.remote.dto.EventRequest
import com.lpavs.caliinda.core.data.repository.mapper.EventMapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepository
@Inject
constructor(
    private val remotreDataSource: CalendarRemoteDataSource,
    private val localDataSource: CalendarLocalDataSource,
    private val settingsRepository: SettingsRepository,
    private val authManager: AuthManager,
    private val eventMapper: EventMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
  private val _loadedDateRange = MutableStateFlow<ClosedRange<LocalDate>?>(null)
  private val _rangeNetworkState = MutableStateFlow<EventNetworkState>(EventNetworkState.Idle)
  private var fetchJobHolder: JobHolder? = null
  private val fetchJobMutex = Mutex()

  private var lastKnownVisibleDate = LocalDate.now()

  private data class JobHolder(val job: Job, val requestedRange: ClosedRange<LocalDate>)

  private var activeFetchJob: Job? = null
  private val managerScope = CoroutineScope(SupervisorJob() + ioDispatcher)
  val rangeNetworkState: StateFlow<EventNetworkState> = _rangeNetworkState.asStateFlow()

  companion object {
    const val INITIAL_LOAD_DAYS_AROUND = 7L
    const val UPDATE_LOAD_DAYS_AROUND = 5L
    const val TRIGGER_PREFETCH_THRESHOLD = 2L
    const val EXPAND_CHUNK_DAYS = 14L
    const val JUMP_DETECTION_BUFFER_DAYS = 10L
    private const val TAG = "CalendarDataManager"
  }

  // --- Секция Предоставление данных ---
  /** Предоставляет Flow событий из БД для указанной даты */
  @OptIn(ExperimentalCoroutinesApi::class)
  fun getEventsFlowForDate(date: LocalDate): Flow<List<EventDto>> {
    return settingsRepository.timeZoneFlow
        .flatMapLatest { timeZoneIdString ->
          val zoneId = parseTimeZone(timeZoneIdString)
          val (startMillis, endMillis) = calculateLocalBounds(date, zoneId)

          localDataSource.getEventsForDateRangeFlow(startMillis, endMillis).map { entityList ->
            entityList
                .filter { entity -> isEventValidForDate(entity, startMillis, endMillis) }
                .map { entity -> eventMapper.mapToDomain(entity, zoneId.toString()) }
          }
        }
        .catch { e ->
          Log.e(TAG, "Error processing events Flow for date $date", e)
          emit(emptyList())
        }
        .flowOn(ioDispatcher)
  }

  /** Безопасный парсинг часового пояса */
  private fun parseTimeZone(timeZoneIdString: String): ZoneId {
    return try {
      ZoneId.of(timeZoneIdString.ifEmpty { ZoneId.systemDefault().id })
    } catch (e: Exception) {
      Log.w(TAG, "Invalid timezone: $timeZoneIdString, using system default", e)
      ZoneId.systemDefault()
    }
  }

  /** Вычисляет границы дня в локальном часовом поясе */
  private fun calculateLocalBounds(date: LocalDate, zoneId: ZoneId): Pair<Long, Long> {
    val startOfDay = date.atStartOfDay(zoneId)
    val endOfDay = startOfDay.plusDays(1)

    return Pair(startOfDay.toInstant().toEpochMilli(), endOfDay.toInstant().toEpochMilli())
  }

  /** Проверяет, валидно ли событие для указанной даты */
  private fun isEventValidForDate(
      entity: CalendarEventEntity,
      startMillis: Long,
      endMillis: Long
  ): Boolean {
    return if (!entity.isAllDay) {
      entity.startTimeMillis < endMillis && entity.endTimeMillis > startMillis
    } else {
      val durationMillis = entity.endTimeMillis - entity.startTimeMillis
      val twentyFourHoursMillis = TimeUnit.HOURS.toMillis(24)
      val toleranceMillis = TimeUnit.MINUTES.toMillis(5)

      (durationMillis >= twentyFourHoursMillis - toleranceMillis) &&
          (durationMillis <= twentyFourHoursMillis + toleranceMillis) &&
          (entity.startTimeMillis < endMillis && entity.endTimeMillis > startMillis)
    }
  }

  // --- Секция Запрос данных ---
  suspend fun fetchAndStoreDateRange(range: ClosedRange<LocalDate>, replace: Boolean) {
    _rangeNetworkState.value = EventNetworkState.Loading
    val result = remotreDataSource.getEvents(range.start, range.endInclusive)
    withContext(ioDispatcher) {
      result
          .onSuccess { dtoList ->
            val zoneIdString =
                settingsRepository.timeZoneFlow.first().ifEmpty { ZoneId.systemDefault().id }
            val zoneId = ZoneId.of(zoneIdString)
            val entities = dtoList.mapNotNull { eventMapper.mapToEntity(it, zoneIdString) }
            val startRangeMillis = range.start.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endRangeMillis =
                range.endInclusive.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            localDataSource.clearAndInsertEventsForRange(
                startRangeMillis = startRangeMillis,
                endRangeMillis = endRangeMillis,
                newEvents = entities)
            updateLoadedRange(range, replace)
            _rangeNetworkState.value = EventNetworkState.Idle
          }
          .onFailure { exception ->
            val errorMessage = exception.message ?: "Unknown error"
            _rangeNetworkState.value = EventNetworkState.Error(errorMessage)
          }
    }
  }

  private fun updateLoadedRange(fetchedRange: ClosedRange<LocalDate>, replace: Boolean) {
    if (replace) {
      _loadedDateRange.value = fetchedRange
      Log.d(TAG, "Replaced loaded range with: $fetchedRange")
    } else {
      _loadedDateRange.update { current ->
        val updatedRange = current?.union(fetchedRange) ?: fetchedRange
        Log.d(
            TAG,
            "Merged fetched range $fetchedRange with current ${current}. New loaded range: $updatedRange")
        updatedRange
      }
    }
  }

  private fun ClosedRange<LocalDate>.union(other: ClosedRange<LocalDate>): ClosedRange<LocalDate> {
    val newStart = minOf(this.start, other.start)
    val newEnd = maxOf(this.endInclusive, other.endInclusive)
    return newStart..newEnd
  }

  /** Принудительно обновляет данные для указанной даты */
  suspend fun refreshDate(centerDateToRefreshAround: LocalDate) {
    Log.d(TAG, "Manual refresh triggered around date: $centerDateToRefreshAround")

    activeFetchJob?.cancel(
        CancellationException("Manual refresh triggered for $centerDateToRefreshAround"))
    Log.d(TAG, "refreshDate: Previous activeFetchJob (ensure...) cancelled (if existed).")

    val targetRefreshRangeStart = centerDateToRefreshAround.minusDays(UPDATE_LOAD_DAYS_AROUND)
    val targetRefreshRangeEnd = centerDateToRefreshAround.plusDays(UPDATE_LOAD_DAYS_AROUND)
    val targetRefreshRange = targetRefreshRangeStart..targetRefreshRangeEnd
    Log.d(TAG, "refreshDate: Target refresh range is $targetRefreshRange")

    fetchJobMutex.withLock {
      fetchJobHolder
          ?.job
          ?.cancel(CancellationException("Force refresh for $centerDateToRefreshAround"))
      fetchJobHolder = null
      Log.d(TAG, "refreshDate: Cancelled existing fetchJobHolder due to force refresh.")
    }

    launchProtectedFetch(targetRefreshRange, true)
  }

  private fun launchProtectedFetch(rangeToFetch: ClosedRange<LocalDate>, replace: Boolean = true) {
    managerScope.launch {
      fetchJobMutex.withLock {
        val currentActiveJobDetails = fetchJobHolder?.takeIf { it.job.isActive }

        if (currentActiveJobDetails != null) {
          val currentlyFetchingRange = currentActiveJobDetails.requestedRange

          if (rangeToFetch.start >= currentlyFetchingRange.start &&
              rangeToFetch.endInclusive <= currentlyFetchingRange.endInclusive) {
            Log.d(
                TAG,
                "launchProtectedFetch: New range $rangeToFetch is covered by ongoing $currentlyFetchingRange. Skipping.")
            return@launch
          }
          if (_rangeNetworkState.value == EventNetworkState.Loading) {
            Log.d(
                TAG,
                "launchProtectedFetch: Network is already Loading (for $currentlyFetchingRange). New request for $rangeToFetch will be deferred. Skipping new launch.")
            return@launch
          }
          Log.d(
              TAG,
              "launchProtectedFetch: New range $rangeToFetch. Cancelling previous job (if any was active but not in Loading state) for $currentlyFetchingRange.")
          currentActiveJobDetails.job.cancel(
              CancellationException("Superseded by new fetch for $rangeToFetch"))
        }

        _rangeNetworkState.value = EventNetworkState.Loading
        Log.i(
            TAG,
            "launchProtectedFetch: Set _rangeNetworkState to Loading. Starting fetch for $rangeToFetch.")

        val newActualFetchJob =
            managerScope.launch {
              try {
                fetchAndStoreDateRange(rangeToFetch.start..rangeToFetch.endInclusive, replace)
              } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                Log.i(
                    TAG,
                    "launchProtectedFetch/newActualFetchJob: Fetch job for $rangeToFetch was cancelled.",
                    e)
                throw e
              } catch (e: Exception) {
                Log.e(
                    TAG,
                    "launchProtectedFetch/newActualFetchJob: Exception in fetch job for $rangeToFetch",
                    e)
                if (_rangeNetworkState.value !is EventNetworkState.Error &&
                    _rangeNetworkState.value != EventNetworkState.Idle) {
                  _rangeNetworkState.value =
                      EventNetworkState.Error("FADR Error unhandled: ${e.message}")
                }
              } finally {
                fetchJobMutex.withLock {
                  if (fetchJobHolder?.job == coroutineContext[Job]) {
                    fetchJobHolder = null
                    Log.d(
                        TAG,
                        "launchProtectedFetch (finally of newActualFetchJob): Cleared fetchJobHolder for $rangeToFetch.")
                  }
                }
                if (_rangeNetworkState.value == EventNetworkState.Loading &&
                    !coroutineContext.isActive) {
                  Log.w(
                      TAG,
                      "launchProtectedFetch (finally of newActualFetchJob): Job for $rangeToFetch ended, but state is still Loading. Resetting to Idle.")
                  _rangeNetworkState.value = EventNetworkState.Idle
                }
              }
            }
        fetchJobHolder = JobHolder(newActualFetchJob, rangeToFetch)
      }
    }
  }

  /** Проверяет, нужно ли загружать/расширять диапазон дат */
  private suspend fun ensureDateRangeLoadedAround(centerDate: LocalDate, forceLoad: Boolean) =
      withContext(ioDispatcher) {
        val currentlyLoaded = _loadedDateRange.value
        val initialOrJumpTargetRange =
            centerDate.minusDays(INITIAL_LOAD_DAYS_AROUND)..centerDate.plusDays(
                    INITIAL_LOAD_DAYS_AROUND)
        Log.d(
            TAG,
            "ensureDateRange: center=$centerDate, current=$currentlyLoaded, initialOrJumpTarget=$initialOrJumpTargetRange, forceLoad=$forceLoad")

        if (forceLoad) {
          Log.i(
              TAG,
              "Force load requested for $centerDate. Fetching range: $initialOrJumpTargetRange")
          fetchJobMutex.withLock {
            fetchJobHolder?.job?.cancel(CancellationException("Force load for $centerDate"))
            fetchJobHolder = null
            Log.d(
                TAG,
                "ensureDateRangeLoadedAround: Cancelled existing fetchJobHolder due to forceLoad.")
          }
          launchProtectedFetch(initialOrJumpTargetRange, true)
          return@withContext
        }

        if (currentlyLoaded == null) {
          Log.i(TAG, "Initial load for $centerDate. Fetching range: $initialOrJumpTargetRange")
          launchProtectedFetch(initialOrJumpTargetRange, true)
        } else {
          val isJump =
              centerDate < currentlyLoaded.start.minusDays(JUMP_DETECTION_BUFFER_DAYS) ||
                  centerDate > currentlyLoaded.endInclusive.plusDays(JUMP_DETECTION_BUFFER_DAYS)

          if (isJump) {
            Log.i(
                TAG, "Jump detected for $centerDate. Fetching new range: $initialOrJumpTargetRange")
            fetchJobMutex.withLock {
              fetchJobHolder?.job?.cancel(CancellationException("Jump detected for $centerDate"))
              fetchJobHolder = null
              Log.d(
                  TAG,
                  "ensureDateRangeLoadedAround: Cancelled existing fetchJobHolder due to jump.")
            }
            launchProtectedFetch(initialOrJumpTargetRange, true)
          } else {
            var rangeToFetchDeltaStart: LocalDate? = null
            var rangeToFetchDeltaEnd: LocalDate? = null

            if (centerDate >= currentlyLoaded.endInclusive.minusDays(TRIGGER_PREFETCH_THRESHOLD)) {
              rangeToFetchDeltaStart = currentlyLoaded.endInclusive.plusDays(1)
              rangeToFetchDeltaEnd = rangeToFetchDeltaStart.plusDays(EXPAND_CHUNK_DAYS - 1)
              Log.i(
                  TAG,
                  "Forward prefetch triggered at $centerDate. Need to fetch DELTA: [$rangeToFetchDeltaStart .. $rangeToFetchDeltaEnd]")
            } else if (centerDate <= currentlyLoaded.start.plusDays(TRIGGER_PREFETCH_THRESHOLD)) {
              rangeToFetchDeltaEnd = currentlyLoaded.start.minusDays(1)
              rangeToFetchDeltaStart = rangeToFetchDeltaEnd.minusDays(EXPAND_CHUNK_DAYS - 1)
              Log.i(
                  TAG,
                  "Backward prefetch triggered at $centerDate. Need to fetch DELTA: [$rangeToFetchDeltaStart .. $rangeToFetchDeltaEnd]")
            }

            if (rangeToFetchDeltaStart != null && rangeToFetchDeltaEnd != null) {
              launchProtectedFetch(rangeToFetchDeltaStart..rangeToFetchDeltaEnd, false)
            } else {
              Log.d(
                  TAG,
                  "No delta load needed for $centerDate. It is comfortably within $currentlyLoaded.")
            }
          }
        }
      }

  // --- Секция CRUD
  suspend fun createEvent(request: EventRequest): Result<Unit> {
    val result = remotreDataSource.createEvent(request)
    if (result.isSuccess) {
      refreshDate(lastKnownVisibleDate)
    }
    return result
  }

  suspend fun deleteEvent(
      eventId: String,
      mode: EventDeleteMode = EventDeleteMode.DEFAULT
  ): Result<Unit> {
    val result = remotreDataSource.deleteEvent(eventId, mode)
    if (result.isSuccess) {
      refreshDate(lastKnownVisibleDate)
    }
    return result
  }

  suspend fun updateEvent(
      eventId: String,
      updateData: EventRequest,
      mode: EventUpdateMode
  ): Result<Unit> {
    val result = remotreDataSource.updateEvent(eventId, mode, updateData)
    if (result.isSuccess) {
      refreshDate(lastKnownVisibleDate)
    }
    return result
  }

  fun setCurrentVisibleDate(newDate: LocalDate, forceRefresh: Boolean = false) {
    Log.d(TAG, "setCurrentVisibleDate: CALLED with $newDate. Last known: $lastKnownVisibleDate")
    val needsDateUpdate = newDate != lastKnownVisibleDate

    if (!needsDateUpdate && !forceRefresh && _loadedDateRange.value != null) {
      Log.d(TAG, "setCurrentVisibleDate: Date is the same, skipping.")
      return
    }
    activeFetchJob?.cancel(
        CancellationException("New date set: $newDate, forceRefresh=$forceRefresh"))
    Log.d(TAG, "setCurrentVisibleDate: Previous activeFetchJob cancelled (if existed).")

    if (needsDateUpdate) {
      lastKnownVisibleDate = newDate
    }
    if (authManager.authState.value.isSignedIn) {
      activeFetchJob = managerScope.launch { ensureDateRangeLoadedAround(newDate, forceRefresh) }
    }
  }

  /**
   * Очищает все локальные данные о событиях в БД. Вызывается при выходе пользователя из системы.
   */
  suspend fun clearLocalDataOnSignOut() {
    Log.i(TAG, "Starting local database clear on sign out...")
    try {
      withContext(ioDispatcher) {
        localDataSource.deleteAllEvents()
        Log.i(TAG, "Local database cleared successfully.")
      }
      _loadedDateRange.value = null
      Log.d(TAG, "Reset _loadedDateRange state.")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to clear local database on sign out", e)
    }
  }
}
