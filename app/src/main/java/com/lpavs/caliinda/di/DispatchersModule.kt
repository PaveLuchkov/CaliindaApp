package com.lpavs.caliinda.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

// Квалификатор для IO Dispatcher
@Retention(AnnotationRetention.BINARY) @Qualifier annotation class IoDispatcher

@Retention(AnnotationRetention.BINARY) @Qualifier annotation class MainDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

  @Provides @Singleton @IoDispatcher fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

  @Provides
  @Singleton
  @MainDispatcher
  fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
