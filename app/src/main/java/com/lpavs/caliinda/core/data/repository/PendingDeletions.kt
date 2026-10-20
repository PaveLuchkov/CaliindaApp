package com.lpavs.caliinda.core.data.repository

import com.lpavs.caliinda.core.data.utils.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Обратный отсчёт удаления: карточка-«призрак» с фразой, таймером и «Отменить». */
data class DeletionCountdown(
    val message: UiText,
    /** Когда начался отсчёт (uptime, мс) — от него карточка считает, сколько осталось. */
    val startedAtMillis: Long,
    val durationMillis: Long,
)

/**
 * Удалённые пользователем события до настоящего удаления из календаря. Сначала — отсчёт
 * ([deleting]): на месте карточки призрак, удаление можно отменить. Потом — [hidden]: карточку
 * убрали, а запись в календарь ещё идёт; без этого она мигнула бы обратно до обновления провайдера.
 */
@Singleton
class PendingDeletions @Inject constructor() {
  private val _deleting = MutableStateFlow<Map<String, DeletionCountdown>>(emptyMap())
  val deleting: StateFlow<Map<String, DeletionCountdown>> = _deleting.asStateFlow()

  private val _hidden = MutableStateFlow<Set<String>>(emptySet())
  val hidden: StateFlow<Set<String>> = _hidden.asStateFlow()

  fun startCountdown(id: String, countdown: DeletionCountdown) = _deleting.update { it + (id to countdown) }

  /** Отменили — карточка возвращается как была. */
  fun cancel(id: String) = _deleting.update { it - id }

  /** Отсчёт истёк — карточка уходит из списка (схлопывается), начинается запись в календарь. */
  fun hide(id: String) {
    _hidden.update { it + id }
    _deleting.update { it - id }
  }

  /** Записали в календарь — провайдер уже без события, скрывать больше нечего. */
  fun release(id: String) = _hidden.update { it - id }
}
