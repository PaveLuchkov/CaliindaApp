package com.lpavs.caliinda.feature.calendar.presentation.intro

import android.content.Context
import androidx.annotation.StringRes
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.feature.calendar.presentation.components.page.ProjectRibbon
import com.lpavs.caliinda.feature.calendar.presentation.model.EventUiModel
import com.lpavs.caliinda.feature.calendar.presentation.model.EventUiModelMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Ненастоящие события для знакомства — уже готовые UI-модели, как в ленте. */
data class IntroSamples(
    /** Шаг «цвет»: прошедшее, идущее сейчас и ближайшее. */
    val timeline: List<EventUiModel>,
    /** Шаг «высота»: короткое и длинное. */
    val durations: List<EventUiModel>,
    /** Шаг «проекты»: лента на странице дня и карточка на экране проектов. */
    val ribbon: ProjectRibbon,
    val project: EventUiModel,
)

/**
 * Собирает примеры через настоящий [EventUiModelMapper] относительно «сейчас»: цвета, высоты,
 * волна прогресса и формат времени в знакомстве всегда те же, что и в приложении.
 */
class IntroSamplesFactory
@Inject
constructor(
    private val mapper: EventUiModelMapper,
    @ApplicationContext private val context: Context,
) {
  fun build(now: Instant, zone: ZoneId): IntroSamples {
    val today = now.atZone(zone).toLocalDate()
    // Ровные минуты, чтобы время на карточках выглядело аккуратно.
    val base = now.truncatedTo(ChronoUnit.MINUTES)

    val timeline =
        mapper.mapToUiModels(
            events =
                listOf(
                    sample("past", R.string.suggested_event_coffee_full, base.minus(95), base.minus(65)),
                    sample("now", R.string.suggested_event_work_full, base.minus(40), base.plus(25)),
                    // Начнётся через полчаса — карточка уже наполовину окрасилась в цвет «сейчас»
                    // (окно перехода — час, cuid.EVENT_TRANSITION_WINDOW_MINUTES).
                    sample("next", R.string.suggested_event_dinner_full, base.plus(35), base.plus(95))),
            zone = zone,
            currentTime = now,
            date = today,
            project = false)

    // Завтра днём, в фиксированные часы: без «сейчас» и «скоро» и без перехода через полночь —
    // видна только разница высот.
    val tomorrow = today.plusDays(1).atTime(10, 0).atZone(zone).toInstant()
    val durations =
        mapper.mapToUiModels(
            events =
                listOf(
                    sample("short", R.string.suggested_event_call_full, tomorrow, tomorrow.plus(15)),
                    sample("long", R.string.suggested_event_study_full, tomorrow.plus(60), tomorrow.plus(270))),
            zone = zone,
            currentTime = now,
            date = today.plusDays(1),
            project = false)

    val tripStart = today.minusDays(2).atStartOfDay(zone).toInstant()
    val trip =
        sample("trip", R.string.suggested_event_travel_full, tripStart, tripStart.plus(Duration.ofDays(7)))
    val project =
        mapper
            .mapToUiModels(
                events = listOf(trip), zone = zone, currentTime = now, date = today, project = true)
            .single()

    return IntroSamples(
        timeline = timeline,
        durations = durations,
        ribbon = ProjectRibbon(event = trip, dayNumber = 3, totalDays = 7),
        project = project)
  }

  private fun sample(id: String, @StringRes title: Int, start: Instant, end: Instant) =
      EventDto(
          id = "intro_$id",
          summary = context.getString(title),
          startTime = start,
          endTime = end)

  private fun Instant.plus(minutes: Int): Instant = plus(Duration.ofMinutes(minutes.toLong()))

  private fun Instant.minus(minutes: Int): Instant = minus(Duration.ofMinutes(minutes.toLong()))
}
