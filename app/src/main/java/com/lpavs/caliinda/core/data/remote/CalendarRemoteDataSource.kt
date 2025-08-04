package com.lpavs.caliinda.core.data.remote

import android.os.Build
import androidx.annotation.RequiresExtension
import com.lpavs.caliinda.app.di.IoDispatcher
import com.lpavs.caliinda.core.common.ApiException
import com.lpavs.caliinda.core.common.NetworkException
import com.lpavs.caliinda.core.common.UnknownException
import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.core.data.remote.dto.EventRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import retrofit2.HttpException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class CalendarRemoteDataSource
@Inject constructor(
    private val apiService: CalendarApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun getEvents(startDate: LocalDate, endDate: LocalDate): Result<List<EventDto>> {
        return safeApiCall {
            val response = apiService.getEventsForRange(
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                throw ApiException(response.code(), response.errorBody()?.string())
            }
        }
    }

    suspend fun createEvent(event: EventRequest): Result<Unit> {
        return safeApiCall {
            val response = apiService.createEvent(event)
            if (response.isSuccessful) {
                Unit
            } else {
                throw Exception("Failed to create event: ${response.code()}")
            }
        }
    }

    suspend fun updateEvent(
        eventId: String,
        mode: EventUpdateMode,
        updateData: EventRequest
    ): Result<Unit> {
        return safeApiCall {
            val response = apiService.updateEvent(
                eventId,
                mode.value,
                updateData,
            )
            if (response.isSuccessful) {
                Unit
            } else {
                throw ApiException(response.code(), response.errorBody()?.string())
            }
        }
    }

    suspend fun deleteEvent(eventId: String, mode: EventDeleteMode): Result<Unit> {
        return safeApiCall {
            val response = apiService.deleteEvent(eventId, mode.value)
            if (response.isSuccessful) {
                Unit
            } else {
                throw ApiException(response.code(), response.errorBody()?.string())
            }
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    private suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
        return withContext(ioDispatcher) {
            try {
                Result.success(apiCall())
            } catch (e: Throwable) {
                when (e) {
                    is IOException -> Result.failure(NetworkException("Network issue"))
                    is HttpException -> {
                        val errorBody = e.response()?.errorBody()?.string()
                        Result.failure(
                            ApiException(
                                e.code(),
                                "Server error: ${e.code()}. $errorBody"
                            )
                        )
                    }

                    is ApiException -> Result.failure(e)
                    else -> Result.failure(UnknownException("Unknown error: ${e.message}"))
                }
            }
        }
    }
}

