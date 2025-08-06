package com.lpavs.caliinda.feature.calendar.data

import android.content.Context
import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.core.ui.util.IDateTimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject

class EventUiModelMapper @Inject constructor(
    private val dateTimeUtils: IDateTimeUtils,
    @ApplicationContext private val context: Context
){
    fun calculateEventDurationMinutes(event: EventDto, currentTimeZoneId: String): Long {
        val start = dateTimeUtils.parseToInstant(event.startTime, currentTimeZoneId)
        val end = dateTimeUtils.parseToInstant(event.endTime, currentTimeZoneId)
        return if (start != null && end != null && end.isAfter(start)) {
            Duration.between(start, end).toMinutes()
        } else {
            0L
        }
    }


}

