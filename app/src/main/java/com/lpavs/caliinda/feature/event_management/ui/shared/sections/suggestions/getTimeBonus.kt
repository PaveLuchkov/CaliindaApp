package com.lpavs.caliinda.feature.event_management.ui.shared.sections.suggestions

import java.time.LocalTime

fun getTimeBasedBonus(chipKey: String, time: LocalTime): Int {
  val priority1Bonus = 100
  val priority2Bonus = 75
  val hour = time.hour
  val key = chipKey

  // Приоритет 1 по времени
  val morningP1 = setOf("breakfast", "gym")
  val dayP1 = setOf("work", "meeting")
  val eveningP1 = setOf("dinner", "relax")
  val nightP1 = setOf("sleep", "relax")

  // Приоритет 2 по времени
  val morningP2 = setOf("walking", "doctor", "pet", "reading", "cooking")
  val dayP2 =
      setOf(
          "lunch", "call", "errand", "appointment", "project", "presentation", "study", "shopping")
  val eveningP2 =
      setOf(
          "relax",
          "movie",
          "date",
          "party",
          "cooking",
          "walking",
          "hobby",
          "gym",
          "reading",
          "pet",
          "call")
  val nightP2 = setOf("reading", "movie", "party", "date", "pet", "cleaning", "hobby")

  return when {
    // Приоритет 1 – проверяется первым
    key in morningP1 && hour in 5..10 -> priority1Bonus
    key in dayP1 && hour in 9..17 -> priority1Bonus
    key in eveningP1 && hour in 18..22 -> priority1Bonus
    key in nightP1 && (hour in 22..23 || hour in 0..1) -> priority1Bonus

    // Приоритет 2 – проверяется только если не сработал приоритет 1
    key in morningP2 && hour in 6..11 -> priority2Bonus
    key in dayP2 && hour in 11..17 -> priority2Bonus
    key in eveningP2 && hour in 17..23 -> priority2Bonus
    key in nightP2 && (hour in 22..23 || hour in 0..3) -> priority2Bonus

    else -> 0
  }
}
