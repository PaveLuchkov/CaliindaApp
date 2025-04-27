package com.example.caliindar.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

// Интерфейс для удобства тестирования (опционально, но хорошая практика)
interface ITimeTicker {
    val currentTime: StateFlow<Instant>
}

@Singleton // Этот класс будет синглтоном в пределах всего приложения
class TimeTicker @Inject constructor() : ITimeTicker { // @Inject constructor для Hilt

    // Создаем scope, который будет жить вместе с синглтоном
    // Используем SupervisorJob, чтобы ошибка в одной корутине не отменила весь scope
    // Dispatchers.Default подходит для фоновых задач типа delay
    private val tickerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Используем StateFlow, чтобы хранить последнее значение и предоставлять его подписчикам
    override val currentTime: StateFlow<Instant> = flow {
        while (true) { // Бесконечный цикл для тикера
            emit(Instant.now()) // Отправляем текущее время
            delay(60000L) // Пауза на 1 минуту (можно вынести в константу)
        }
    }.stateIn( // Преобразуем холодный Flow в горячий StateFlow
        scope = tickerScope, // Запускаем Flow в нашем scope
        started = SharingStarted.WhileSubscribed(5000), // Начинаем эмитить, когда есть подписчики, останавливаемся через 5 сек после последнего
        initialValue = Instant.now() // Начальное значение
    )

    // Важно: Hilt управляет жизненным циклом @Singleton,
    // поэтому нам не нужно явно отменять tickerScope вручную,
    // Hilt сделает это при уничтожении ApplicationComponent.
}


@Module
@InstallIn(SingletonComponent::class) // Устанавливаем в компонент приложения
abstract class TimeTickerModule {

    // Используем @Binds для предоставления реализации интерфейса
    // Hilt знает, как создать TimeTicker (@Inject constructor),
    // и свяжет запрос ITimeTicker с экземпляром TimeTicker.
    @Binds
    @Singleton // Убедимся, что связывание тоже синглтонное
    abstract fun bindTimeTicker(
        timeTicker: TimeTicker // Реализация
    ): ITimeTicker // Интерфейс
}