package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system

import com.lpavs.caliinda.feature.calendar.presentation.intro.IntroSamples

/** Шаги знакомства — каждый показывает одну идею на живых карточках. */
enum class IntroStep {
  /** Цвет — это время: прошло, идёт, скоро. */
  COLORS,
  /** Высота — это длительность. */
  HEIGHT,
  /** Многодневное — на соседнем экране, в дне — лентой. */
  PROJECTS,
  /** Создать первое событие. */
  START;

  companion object {
    fun next(current: IntroStep): IntroStep? = entries.getOrNull(current.ordinal + 1)
  }
}

data class IntroState(
    val currentStep: IntroStep = IntroStep.COLORS,
    val isFinished: Boolean = false,
    /** Примеры событий для шагов; null — ещё не собраны. */
    val samples: IntroSamples? = null,
)
