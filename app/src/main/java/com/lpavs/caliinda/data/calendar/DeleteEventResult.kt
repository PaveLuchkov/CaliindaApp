package com.lpavs.caliinda.data.calendar

sealed interface DeleteEventResult {
  object Idle : DeleteEventResult // Начальное состояние или после обработки результата

  object Loading : DeleteEventResult // Процесс удаления запущен

  object Success : DeleteEventResult // Событие успешно удалено (и на бэке, и локально)

  data class Error(val message: String) : DeleteEventResult // Произошла ошибка
}
