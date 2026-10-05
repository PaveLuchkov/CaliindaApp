package com.lpavs.caliinda.core.ui.di

import com.lpavs.caliinda.core.ui.util.DateTimeFormatterUtilImpl
import com.lpavs.caliinda.core.ui.util.IDateTimeFormatterUtil
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UiModule {

  @Binds
  @Singleton
  abstract fun bindDateTimeFormatterUtil(impl: DateTimeFormatterUtilImpl): IDateTimeFormatterUtil
}
