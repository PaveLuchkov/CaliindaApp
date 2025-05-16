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

@Singleton
class TimeTicker @Inject constructor() : ITimeTicker { // @Inject constructor для Hilt

    private val tickerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val currentTime: StateFlow<Instant> = flow {
        while (true) {
            emit(Instant.now())
            delay(60000L)
        }
    }.stateIn(
        scope = tickerScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Instant.now()
    )
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