package com.lpavs.caliinda.feature.calendar.presentation.components.events.lists

import androidx.compose.runtime.Composable
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.CalendarAccessEvent
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.IntroStep
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.IntroductionEvent

@Composable
fun SystemEventsList(
    hasCalendarAccess: Boolean,
    onGrantAccessClick: () -> Unit,
    introStep: IntroStep,
    projectView: Boolean,
    onIntroNext: () -> Unit,
) {
  if (hasCalendarAccess) {
    IntroductionEvent(introStep = introStep, projectView = projectView, onIntroNext = onIntroNext)
  } else {
    CalendarAccessEvent(onGrantAccessClick = onGrantAccessClick)
  }
}
