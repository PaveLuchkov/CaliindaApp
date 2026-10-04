package com.lpavs.caliinda.core.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * События, которые пользователь удалил, но ещё может вернуть кнопкой «Отменить». Экраны прячут
 * их сразу, а в календаре они удаляются, когда окно отмены закрылось.
 */
@Singleton
class PendingDeletions @Inject constructor() {
  private val _ids = MutableStateFlow<Set<String>>(emptySet())
  val ids: StateFlow<Set<String>> = _ids.asStateFlow()

  fun add(id: String) = _ids.update { it + id }

  fun remove(id: String) = _ids.update { it - id }
}
