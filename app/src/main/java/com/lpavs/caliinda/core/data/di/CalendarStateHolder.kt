package com.lpavs.caliinda.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface ICalendarStateHolder {
  val currentVisibleDate: StateFlow<LocalDate>

  fun setCurrentVisibleDate(newDate: LocalDate)
}

@Singleton
class CalendarStateHolder @Inject constructor() : ICalendarStateHolder {
  private val _currentVisibleDate = MutableStateFlow(LocalDate.now())
  override val currentVisibleDate: StateFlow<LocalDate> = _currentVisibleDate

  override fun setCurrentVisibleDate(newDate: LocalDate) {
    _currentVisibleDate.value = newDate
  }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CalendarState {

  @Binds
  @Singleton
  abstract fun bindCalendarStateHolder(impl: CalendarStateHolder): ICalendarStateHolder
}
