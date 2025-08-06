package com.lpavs.caliinda.feature.calendar.data

import android.content.Context
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.core.ui.util.IDateTimeFormatterUtil
import com.lpavs.caliinda.core.ui.util.IDateTimeUtils
import com.lpavs.caliinda.feature.calendar.data.EventUiModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.exp

class EventUiDetailsModelMapper @Inject constructor(
    private val dateTimeUtils: IDateTimeUtils,
    private val dateTimeFormatterUtil: IDateTimeFormatterUtil,
    @ApplicationContext private val context: Context
) {
    fun mapToUiModels(
        event: EventDto,
        timeZoneId: String,
        currentTime: Instant,
    ): EventDetailsUiModel {
        val startInstant = dateTimeUtils.parseToInstant(event.startTime, timeZoneId)
        val endInstant = dateTimeUtils.parseToInstant(event.endTime, timeZoneId)
        val isCurrent = startInstant != null && endInstant != null &&
                !currentTime.isBefore(startInstant) && currentTime.isBefore(endInstant)

        val eventDetailsUiModel = EventDetailsUiModel(
            summary = event.summary,
            formattedTimeString = dateTimeFormatterUtil.formatEventDetailsTime(
                context,
                event,
                timeZoneId,
                ConfigurationCompat.getLocales(context.resources.configuration).get(0) ?: java.util.Locale.getDefault()
            ),
            isCurrent = isCurrent,
            originalEvent = event
        )
        return eventDetailsUiModel
    }
}
