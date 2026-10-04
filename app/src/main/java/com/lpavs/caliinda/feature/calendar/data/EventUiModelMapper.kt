package com.lpavs.caliinda.feature.calendar.data

import android.content.Context
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.core.ui.util.IDateTimeFormatterUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
      timeZoneId: String,
      currentTime: Instant,
      date: LocalDate,
      project: Boolean
  ): List<EventUiModel> {
    val sortedEvents =
        (if (project) events else events.filter { !it.isAllDay }).sortedBy { it.startTime }
    val isToday = date == currentTime.atZone(ZoneId.of(timeZoneId)).toLocalDate()
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
      // «Осталось дней» — только для уже идущих проектов; у предстоящих видна дата начала.
      val daysLeft =
          if (project && !currentTime.isBefore(startInstant)) {
            Duration.between(currentTime, endInstant).toDays()
          } else {
            null
          }
      val baseHeight = calculateEventHeight(
          durationMinutes,
          isMicroEvent,
          isProject = project,
          startInstant = startInstant,
          endInstant =endInstant,
          currentTime = currentTime
      )

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
              summary = event.summary,
              location = event.location,
              isAllDay = event.isAllDay,
              formattedTimeString =
                  dateTimeFormatterUtil.formatEventListTime(
                      context,
                      event,
                      timeZoneId,
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
              daysLeft = daysLeft
              )
      event
    }
  }

    private fun calculateEventHeight(
        durationMinutes: Long,
        isMicroEvent: Boolean,
        isProject: Boolean,
        startInstant: Instant,
        endInstant: Instant,
        currentTime: Instant
    ): Dp {

        if (isMicroEvent) return cuid.MicroEventHeight

        val minHeight = cuid.MinEventHeight
        val maxHeight = cuid.MaxEventHeight

        val durationDouble = durationMinutes.toDouble()
        val heightRange = maxHeight - minHeight

        val isNotProjectType = durationMinutes / 60 < 24

        val midpoint =
            if (isNotProjectType) cuid.HeightSigmoidMidpointMinutes
            else cuid.HeightSigmoidProjectMidpointMinutes

        val steepness =
            if (isNotProjectType) cuid.HeightSigmoidSteepness
            else cuid.HeightSigmoidProjectSteepness

        val scaleFactor =
            if (isNotProjectType) cuid.HeightSigmoidScaleFactor
            else cuid.HeightSigmoidProjectScaleFactor

        val x = (durationDouble - midpoint) / scaleFactor
        val sigmoidOutput = 1.0 / (1.0 + exp(-steepness * x))

        val baseHeight = minHeight + (heightRange * sigmoidOutput.toFloat())

        if (!isProject)
            return baseHeight.coerceIn(minHeight, maxHeight)

        // 🔥 Новая логика для project

        val totalDuration = Duration.between(startInstant, endInstant).toMillis()
        if (totalDuration <= 0) return minHeight

        val elapsed = Duration.between(startInstant, currentTime).toMillis()
            .coerceIn(0, totalDuration)

        val progress = elapsed.toFloat() / totalDuration.toFloat()

        // линейное уменьшение от 1.0 до 0.0
        val progressFactor = 1f - progress

        val scaledHeight = minHeight + (baseHeight - minHeight) * progressFactor

        return scaledHeight.coerceIn(minHeight, maxHeight)
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
