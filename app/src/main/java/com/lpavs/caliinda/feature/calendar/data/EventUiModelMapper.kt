package com.lpavs.caliinda.feature.calendar.data

import android.content.Context
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.core.ui.util.IDateTimeFormatterUtil
import com.lpavs.caliinda.core.ui.util.IDateTimeUtils
import com.lpavs.caliinda.feature.calendar.ui.components.events.EventUiModel
import com.lpavs.caliinda.feature.calendar.ui.components.events.GeneratedShapeParams
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.exp

class EventUiModelMapper @Inject constructor(
    private val dateTimeUtils: IDateTimeUtils,
    private val dateTimeFormatterUtil: IDateTimeFormatterUtil,
    @ApplicationContext private val context: Context
) {
    fun mapToUiModels(
        events: List<EventDto>,
        timeZoneId: String,
        currentTime: Instant,
        date: LocalDate
    ): List<EventUiModel> {
        val sortedEvents = events
            .filter { !it.isAllDay }
            .sortedBy { event ->
                dateTimeUtils.parseToInstant(event.startTime, timeZoneId) ?: Instant.MAX
            }
        val isToday = date == LocalDate.now()
        val nextStartTime: Instant? = if (!isToday) {
            null
        } else {
            sortedEvents.firstNotNullOfOrNull { event ->
                val start = dateTimeUtils.parseToInstant(event.startTime, timeZoneId)
                if (start != null && start.isAfter(currentTime)) start else null
            }
        }
        return sortedEvents.map { event ->
            val startInstant = dateTimeUtils.parseToInstant(event.startTime, timeZoneId)
            val endInstant = dateTimeUtils.parseToInstant(event.endTime, timeZoneId)
            val durationMinutes =
                if (startInstant != null && endInstant != null && endInstant.isAfter(startInstant)) {
                    Duration.between(startInstant, endInstant).toMinutes()
                } else {
                    0L
                }
            val isMicroEvent =
                durationMinutes > 0 && durationMinutes <= cuid.MicroEventMaxDurationMinutes

            val baseHeight = calculateEventHeight(durationMinutes, isMicroEvent)

            val buttonsRowHeight = 56.dp
            val expandedAdditionalHeight =
                if (isMicroEvent && baseHeight < buttonsRowHeight * 1.5f) {
                    buttonsRowHeight * 1.2f
                } else {
                    buttonsRowHeight
                }
            val expandedHeight = if (durationMinutes > 120) {
                (baseHeight + expandedAdditionalHeight * 0.9f).coerceAtLeast(baseHeight)
            } else {
                baseHeight + expandedAdditionalHeight
            }
            val transitionWindowDurationMillis =
                Duration.ofMinutes(cuid.EVENT_TRANSITION_WINDOW_MINUTES).toMillis()
            val isCurrent = startInstant != null && endInstant != null &&
                    !currentTime.isBefore(startInstant) && currentTime.isBefore(endInstant)

            val isNext =
                if (nextStartTime == null) false else (startInstant != null && startInstant == nextStartTime)

            val proximityRatio =
                if (!isToday || startInstant == null || currentTime.isAfter(startInstant)) {
                    0f
                } else {
                    val timeUntilStartMillis =
                        Duration.between(currentTime, startInstant).toMillis()
                    if (timeUntilStartMillis > transitionWindowDurationMillis || transitionWindowDurationMillis <= 0) {
                        0f
                    } else {
                        (1.0f - (timeUntilStartMillis.toFloat() / transitionWindowDurationMillis.toFloat())).coerceIn(
                            0f,
                            1f
                        )
                    }
                }

            // Создаем и возвращаем готовую UI-модель
            EventUiModel(
                id = event.id,
                summary = event.summary,
                location = event.location,
                isAllDay = event.isAllDay,
                formattedTimeString = dateTimeFormatterUtil.formatEventListTime(
                    context,
                    event,
                    timeZoneId
                ),
                durationMinutes = durationMinutes,
                isMicroEvent = isMicroEvent,
                baseHeight = baseHeight,
                expandedHeight = expandedHeight,
                isCurrent = isCurrent,
                isNext = isNext,
                proximityRatio = proximityRatio,
                shapeParams = generateShapeParams(event.id), // Твой генератор фигур
                originalEvent = event
            )
        }
    }


    private fun calculateEventHeight(durationMinutes: Long, isMicroEvent: Boolean): Dp {
        return if (isMicroEvent) {
            cuid.MicroEventHeight
        } else {
            val minHeight = cuid.MinEventHeight
            val maxHeight = cuid.MaxEventHeight
            val durationDouble = durationMinutes.toDouble()
            val heightRange = maxHeight - minHeight

            val x =
                (durationDouble - cuid.HeightSigmoidMidpointMinutes) / cuid.HeightSigmoidScaleFactor
            val k = cuid.HeightSigmoidSteepness
            val sigmoidOutput = 1.0 / (1.0 + exp(-k * x))

            val calculatedHeight = minHeight + (heightRange * sigmoidOutput.toFloat())
            calculatedHeight.coerceIn(minHeight, maxHeight)
        }
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
            offestParam = offsetParam
        )
    }

    fun lerpOkLab(start: Color, stop: Color, fraction: Float): Color {
        val startOklab = start.convert(ColorSpaces.Oklab)
        val stopOklab = stop.convert(ColorSpaces.Oklab)

        val l = startOklab.component1() + (stopOklab.component1() - startOklab.component1()) * fraction
        val a = startOklab.component2() + (stopOklab.component2() - startOklab.component2()) * fraction
        val b = startOklab.component3() + (stopOklab.component3() - startOklab.component3()) * fraction
        val alpha = startOklab.alpha + (stopOklab.alpha - startOklab.alpha) * fraction

        return Color(l, a, b, alpha, ColorSpaces.Oklab).convert(ColorSpaces.Srgb)
    }

}

