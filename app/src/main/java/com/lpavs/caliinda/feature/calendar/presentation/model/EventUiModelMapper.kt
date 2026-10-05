package com.lpavs.caliinda.feature.calendar.presentation.model

import com.lpavs.caliinda.core.ui.util.displayTitle
import android.content.Context
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.core.ui.util.IDateTimeFormatterUtil
import com.lpavs.caliinda.core.ui.util.projectHeightFraction
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.exp

class EventUiModelMapper
@Inject
constructor(
    private val dateTimeFormatterUtil: IDateTimeFormatterUtil,
    @ApplicationContext private val context: Context
) {
  fun mapToUiModels(
      events: List<EventDto>,
      zone: ZoneId,
      currentTime: Instant,
      date: LocalDate,
      project: Boolean
  ): List<EventUiModel> {
    val sortedEvents =
        (if (project) events else events.filter { !it.isAllDay }).sortedBy { it.startTime }
    val isToday = date == currentTime.atZone(zone).toLocalDate()
    val nextStartTime: Instant? =
        if (!isToday) {
          null
        } else {
          sortedEvents.firstOrNull { it.startTime.isAfter(currentTime) }?.startTime
        }
    return sortedEvents.map { event ->
      val startInstant = event.startTime
      val endInstant = event.endTime
      val durationMinutes =
          if (endInstant.isAfter(startInstant)) Duration.between(startInstant, endInstant).toMinutes()
          else 0L
      val isMicroEvent = durationMinutes > 0 && durationMinutes <= cuid.MicroEventMaxDurationMinutes
      // «Осталось дней» — только для идущих проектов: у предстоящих видна дата начала, у
      // прошедших (история на страницах прошлых месяцев) считать уже нечего.
      // Считаем календарные дни включая сегодняшний, поэтому в последний день будет 1, а не 0.
      val daysLeft =
          if (project && !currentTime.isBefore(startInstant) && currentTime.isBefore(endInstant)) {
            val lastDay = endInstant.minusNanos(1).atZone(zone).toLocalDate()
            (ChronoUnit.DAYS.between(currentTime.atZone(zone).toLocalDate(), lastDay) + 1)
                .coerceAtLeast(1)
          } else {
            null
          }
      val baseHeight = calculateEventHeight(durationMinutes, isMicroEvent, isProject = project)

      val buttonsRowHeight = 56.dp
      val expandedAdditionalHeight =
          if (isMicroEvent && baseHeight < buttonsRowHeight * 1.5f) {
            buttonsRowHeight * 1.2f
          } else {
            buttonsRowHeight
          }
      val expandedHeight =
          if (durationMinutes > 120) {
            (baseHeight + expandedAdditionalHeight * 0.9f).coerceAtLeast(baseHeight)
          } else {
            baseHeight + expandedAdditionalHeight
          }
      val transitionWindowDurationMillis =
          Duration.ofMinutes(cuid.EVENT_TRANSITION_WINDOW_MINUTES).toMillis()
      val isCurrent = !currentTime.isBefore(startInstant) && currentTime.isBefore(endInstant)
      val isNext =
          if (nextStartTime == null) false
          else startInstant == nextStartTime

      val proximityRatio =
          if (!isToday || currentTime.isAfter(startInstant)) {
            0f
          } else {
            val timeUntilStartMillis = Duration.between(currentTime, startInstant).toMillis()
            if (timeUntilStartMillis > transitionWindowDurationMillis ||
                transitionWindowDurationMillis <= 0) {
              0f
            } else {
              (1.0f - (timeUntilStartMillis.toFloat() / transitionWindowDurationMillis.toFloat()))
                  .coerceIn(0f, 1f)
            }
          }

      // Создаем и возвращаем готовую UI-модель
      val event =
          EventUiModel(
              id = event.id,
              summary = event.displayTitle(context),
              location = event.location,
              isAllDay = event.isAllDay,
              formattedTimeString =
                  dateTimeFormatterUtil.formatEventListTime(
                      context,
                      event,
                      zone,
                      project = project,
                      locale =
                          ConfigurationCompat.getLocales(context.resources.configuration).get(0)
                              ?: java.util.Locale.getDefault()),
              durationMinutes = durationMinutes,
              isMicroEvent = isMicroEvent,
              baseHeight = baseHeight,
              expandedHeight = expandedHeight,
              isCurrent = isCurrent,
              isNext = isNext,
              proximityRatio = proximityRatio,
              shapeParams = generateShapeParams(event.id), // Твой генератор фигур
              originalEvent = event,
              daysLeft = daysLeft,
              progress = timeProgress(startInstant, endInstant, currentTime),
              )
      event
    }
  }

    private fun calculateEventHeight(
        durationMinutes: Long,
        isMicroEvent: Boolean,
        isProject: Boolean,
    ): Dp {
        if (isMicroEvent) return cuid.MicroEventHeight

        val minHeight = cuid.MinEventHeight
        val maxHeight = cuid.MaxEventHeight

        // Проект: высота только от длительности — не от того, сколько уже прошло, иначе
        // ещё не начавшийся проект оказывался выше идущего той же длины.
        if (isProject) return minHeight + (maxHeight - minHeight) * projectHeightFraction(durationMinutes)

        val x = (durationMinutes - cuid.HeightSigmoidMidpointMinutes) / cuid.HeightSigmoidScaleFactor
        val sigmoidOutput = 1.0 / (1.0 + exp(-cuid.HeightSigmoidSteepness * x))
        return (minHeight + (maxHeight - minHeight) * sigmoidOutput.toFloat())
            .coerceIn(minHeight, maxHeight)
    }

    private fun timeProgress(start: Instant, end: Instant, now: Instant): Float {
        val total = Duration.between(start, end).toMillis()
        if (total <= 0) return if (now.isBefore(start)) 0f else 1f
        return (Duration.between(start, now).toMillis().toFloat() / total).coerceIn(0f, 1f)
    }

    fun generateShapeParams(eventId: String): GeneratedShapeParams {
    val hashCode = eventId.hashCode()
    val absHashCode = abs(hashCode)

    val numVertices = (absHashCode % cuid.ShapeMaxVerticesDelta) + cuid.ShapeMinVertices

    val shadowOffsetXSeed = absHashCode % cuid.ShapeShadowOffsetXMaxModulo
    val shadowOffsetYSeed =
        absHashCode % cuid.ShapeShadowOffsetYMaxModulo + cuid.ShapeShadowOffsetYMin

    val offsetParam = (absHashCode % 4 + 1) * cuid.ShapeOffsetParamMultiplier

    val radiusBaseHash = absHashCode / 3 + 42
    val radiusSeed =
        ((radiusBaseHash % cuid.ShapeRadiusSeedRangeModulo) * cuid.ShapeRadiusSeedRange) +
            cuid.ShapeRadiusSeedMin
    val coercedRadiusSeed = radiusSeed.coerceIn(cuid.ShapeRadiusSeedMin, cuid.ShapeMaxRadius)

    val angleSeed = (abs(hashCode) / 5 - 99).mod(cuid.ShapeRotationMaxDegrees)
    val rotationAngle = (angleSeed + cuid.ShapeRotationOffsetDegrees)

    return GeneratedShapeParams(
        numVertices = numVertices,
        radiusSeed = coercedRadiusSeed,
        rotationAngle = rotationAngle,
        shadowOffsetXSeed = shadowOffsetXSeed.dp,
        shadowOffsetYSeed = shadowOffsetYSeed.dp,
        offestParam = offsetParam)
  }
}

data class GeneratedShapeParams(
    val numVertices: Int,
    val radiusSeed: Float,
    val rotationAngle: Float,
    val shadowOffsetYSeed: Dp,
    val shadowOffsetXSeed: Dp,
    val offestParam: Float,
)
