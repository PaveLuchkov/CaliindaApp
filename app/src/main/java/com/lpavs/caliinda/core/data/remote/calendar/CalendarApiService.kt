package com.lpavs.caliinda.core.data.remote.calendar

import com.lpavs.caliinda.core.data.remote.calendar.dto.EventDto
import com.lpavs.caliinda.core.data.remote.calendar.dto.EventRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CalendarApiService {
  @GET("calendar/events/range")
  suspend fun getEventsForRange(
      @Header("Authorization") token: String,
      @Query("startDate") startDate: String,
      @Query("endDate") endDate: String
  ): List<EventDto>

  @POST("calendar/events")
  suspend fun createEvent(
      @Header("Authorization") token: String,
      @Body event: EventRequest
  ): Response<Unit>

  @PATCH("calendar/events/{eventId}")
  suspend fun updateEvent(
      @Header("Authorization") token: String,
      @Path("eventId") eventId: String,
      @Query("update_mode") mode: String,
      @Body updateData: EventRequest
  ): Response<Unit>

  @DELETE("calendar/events/{eventId}")
  suspend fun deleteEvent(
      @Header("Authorization") token: String,
      @Path("eventId") eventId: String,
      @Query("mode") mode: String
  ): Response<Unit>
}