package com.lpavs.caliinda.feature.calendar.presentation.components.events.lists

import androidx.compose.runtime.Composable
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.CalendarAccessEvent
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.IntroState
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.IntroductionEvent
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.IntroductionProjectsHint

/** Служебное вместо событий: запрос доступа к календарю или знакомство. */
@Composable
fun SystemEventsList(
    hasCalendarAccess: Boolean,
    onGrantAccessClick: () -> Unit,
    introState: IntroState,
    projectView: Boolean,
    onIntroNext: () -> Unit = {},
    onIntroFinish: () -> Unit = {},
    onCreateEvent: () -> Unit = {},
) {
  when {
    !hasCalendarAccess -> CalendarAccessEvent(onGrantAccessClick = onGrantAccessClick)
    projectView -> IntroductionProjectsHint()
    else ->
        IntroductionEvent(
            state = introState,
            onNext = onIntroNext,
            onSkip = onIntroFinish,
            onCreateEvent = {
              onIntroFinish()
              onCreateEvent()
            })
  }
}
