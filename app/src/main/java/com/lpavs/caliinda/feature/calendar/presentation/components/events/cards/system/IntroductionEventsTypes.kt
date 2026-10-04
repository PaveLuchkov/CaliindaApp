package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system

enum class IntroStep {
    WELCOME,
    CALENDAR,
    VIEWS,
    EVENTS,
    START;
    companion object {
        fun next(current: IntroStep): IntroStep? {
            val steps = entries
            val currentIndex = steps.indexOf(current)
            return steps.getOrNull(currentIndex + 1)
        }
    }
}

data class IntroState(
    val currentStep: IntroStep = IntroStep.WELCOME,
    val isFinished: Boolean = false
)