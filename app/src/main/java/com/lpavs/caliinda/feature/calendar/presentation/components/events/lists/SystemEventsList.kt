package com.lpavs.caliinda.feature.calendar.presentation.components.events.lists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.IntroStep
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.IntroductionEvent
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.LogInEvent

@Composable
fun SystemEventsList(
    isSignedIn: Boolean,
    onSignInClick: () -> Unit,
    introStep: IntroStep,
    projectView: Boolean,
    onIntroNext: () -> Unit,
) {
  if (isSignedIn) {
          IntroductionEvent(
              introStep = introStep, projectView = projectView, onIntroNext = onIntroNext)

  } else {
    LogInEvent(
        onSignInClick = onSignInClick,
    )
  }
}
