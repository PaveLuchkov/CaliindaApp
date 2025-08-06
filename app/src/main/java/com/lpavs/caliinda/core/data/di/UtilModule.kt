package com.lpavs.caliinda.core.data.di

import com.lpavs.caliinda.core.ui.util.DateTimeFormatterUtilImpl
import com.lpavs.caliinda.core.ui.util.DateTimeUtilsImpl
import com.lpavs.caliinda.core.ui.util.IDateTimeFormatterUtil
import com.lpavs.caliinda.core.ui.util.IDateTimeUtils
import com.lpavs.caliinda.feature.calendar.ui.components.FunMessagesImpl
import com.lpavs.caliinda.feature.calendar.ui.components.IFunMessages
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UtilModule {

    @Binds
    @Singleton
    abstract fun bindDateTimeUtils(
        impl: DateTimeUtilsImpl
    ): IDateTimeUtils

    @Binds
    @Singleton
    abstract fun bindDateTimeFormatterUtil(
        impl: DateTimeFormatterUtilImpl
    ): IDateTimeFormatterUtil

    @Binds
    @Singleton
    abstract fun bindFunMessages(
        impl: FunMessagesImpl
    ): IFunMessages
}