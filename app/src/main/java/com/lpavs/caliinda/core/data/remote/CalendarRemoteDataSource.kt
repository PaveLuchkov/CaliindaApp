package com.lpavs.caliinda.core.data.remote

import android.util.Log
import com.lpavs.caliinda.app.di.IoDispatcher
import com.lpavs.caliinda.core.common.ApiException
import com.lpavs.caliinda.core.common.NetworkException
import com.lpavs.caliinda.core.common.UnknownException
import com.lpavs.caliinda.core.data.auth.AuthManager
import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.core.data.remote.dto.EventRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "CalendarRemoteDataSource"

class CalendarRemoteDataSource
@Inject
constructor(
    private val apiService: CalendarApiService,
    private val authManager: AuthManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
  private suspend inline fun <T> authenticatedApiCall(
      crossinline apiCall: suspend (String) -> T
  ): Result<T> {
    val token = authManager.getBackendAuthToken()
    return if (token != null) {
      safeApiCall { apiCall("Bearer $token") }
    } else {
      Log.e(TAG, "Could not get fresh token")
      Result.failure(Exception("Authorization needed"))
    }
  }

  suspend fun getEvents(startDate: LocalDate, endDate: LocalDate): Result<List<EventDto>> {
    Log.d(TAG, "getEvents called with startDate: $startDate, endDate: $endDate")
    return authenticatedApiCall { token ->
      Log.d(TAG, "Fetching events from API for range: $startDate - $endDate")
      val events =
          apiService.getEventsForRange(
              token = token,
              startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
              endDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
      Log.d(TAG, "getEvents successful, events count: ${events.size}")
      events
    }
  }

  suspend fun createEvent(event: EventRequest): Result<Unit> {
    Log.d(TAG, "createEvent called with event: $event")
    return authenticatedApiCall { token ->
      Log.d(TAG, "Creating event via API: $event")
      apiService.createEvent(token, event)
      Log.d(TAG, "createEvent successful")
    }
  }

  suspend fun updateEvent(
      eventId: String,
      mode: EventUpdateMode,
      updateData: EventRequest
  ): Result<Unit> {
    Log.d(TAG, "updateEvent called with eventId: $eventId, mode: $mode, updateData: $updateData")
    return authenticatedApiCall { token ->
      Log.d(TAG, "Updating event via API: eventId=$eventId, mode=$mode, data=$updateData")
      val response =
          apiService.updateEvent(
              token,
              eventId,
              mode.value,
              updateData,
          )
      if (response.isSuccessful) {
        Log.d(TAG, "updateEvent successful")
        Unit
      } else {
        Log.e(
            TAG,
            "updateEvent failed: ${response.code()}, errorBody: ${response.errorBody()?.string()}")
        throw ApiException(response.code(), response.errorBody()?.string())
      }
    }
  }

  suspend fun deleteEvent(eventId: String, mode: EventDeleteMode): Result<Unit> {
    Log.d(TAG, "deleteEvent called with eventId: $eventId, mode: $mode")
    return authenticatedApiCall { token ->
      Log.d(TAG, "Deleting event via API: eventId=$eventId, mode=$mode")
      val response = apiService.deleteEvent(token, eventId, mode.value)
      Log.d(TAG, "deleteEvent response: $response")
      if (response.isSuccessful) {
        Log.d(TAG, "deleteEvent successful")
        Unit
      } else {
        Log.e(
            TAG,
            "deleteEvent failed: ${response.code()}, errorBody: ${response.errorBody()?.string()}")
        throw ApiException(response.code(), response.errorBody()?.string())
      }
    }
  }

  private suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
    Log.d(TAG, "safeApiCall started")
    return withContext(ioDispatcher) {
      try {
        Log.d(TAG, "safeApiCall executing apiCall")
        val result = apiCall()
        Log.d(TAG, "safeApiCall apiCall successful, result: $result")
        Result.success(result)
      } catch (e: Throwable) {
        Log.e(TAG, "safeApiCall caught exception: ${e.javaClass.simpleName}", e)
        when (e) {
          is IOException -> {
            Log.e(TAG, "safeApiCall: IOException - Network issue", e)
            Result.failure(NetworkException("Network issue: ${e.message}"))
          }
          is HttpException -> {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "safeApiCall: HttpException - code: ${e.code()}, errorBody: $errorBody", e)
            Result.failure(ApiException(e.code(), "Server error: ${e.code()}. $errorBody"))
          }
          is ApiException -> {
            Log.e(TAG, "safeApiCall: ApiException - code: ${e}, message: ${e}", e)
            Result.failure(e)
          }
          else -> {
            Log.e(TAG, "safeApiCall: UnknownException - message: ${e.message}", e)
            Result.failure(UnknownException("Unknown error: ${e.message}"))
          }
        }
      }
    }
  }
}
