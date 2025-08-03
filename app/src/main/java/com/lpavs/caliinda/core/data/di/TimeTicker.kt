package com.lpavs.caliinda.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface ITimeTicker {
  val currentTime: StateFlow<Instant>
}

@Singleton
class TimeTicker @Inject constructor() : ITimeTicker {

  private val tickerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  override val currentTime: StateFlow<Instant> =
      flow {
            while (true) {
              emit(Instant.now())
              delay(60000L)
            }
          }
          .stateIn(
              scope = tickerScope,
              started = SharingStarted.WhileSubscribed(5000),
              initialValue = Instant.now())
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TimeTickerModule {
  // НЕ УДАЛЯТЬ МОДУЛЬ
  @Binds @Singleton abstract fun bindTimeTicker(timeTicker: TimeTicker): ITimeTicker
}
