package com.lpavs.caliinda.feature.calendar.presentation.model

import com.lpavs.caliinda.core.ui.util.displayTitle
import android.content.Context
import androidx.core.os.ConfigurationCompat
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.ui.util.IDateTimeFormatterUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class EventUiDetailsModelMapper
@Inject
constructor(
    private val dateTimeFormatterUtil: IDateTimeFormatterUtil,
    @ApplicationContext private val context: Context
) {
  fun mapToUiModels(
      event: EventDto,
      zone: ZoneId,
      currentTime: Instant,
  ): EventDetailsUiModel {
    val startInstant = event.startTime
    val endInstant = event.endTime
    val isCurrent = !currentTime.isBefore(startInstant) && currentTime.isBefore(endInstant)

    val eventDetailsUiModel =
        EventDetailsUiModel(
            summary = event.displayTitle(context),
            formattedTimeString =
                dateTimeFormatterUtil.formatEventDetailsTime(
                    context,
                    event,
                    zone,
                    ConfigurationCompat.getLocales(context.resources.configuration).get(0)
                        ?: java.util.Locale.getDefault()),
            isCurrent = isCurrent,
            originalEvent = event,
            isProject = event.isProject())
    return eventDetailsUiModel
  }
}

/** Как в репозитории: all-day от двух дней или событие со временем от суток. */
private fun EventDto.isProject(): Boolean {
  val length = Duration.between(startTime, endTime)
  return if (isAllDay) length.toDays() > 1 else length >= Duration.ofDays(1)
}
