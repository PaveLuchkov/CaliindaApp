package com.lpavs.caliinda.feature.event_management.di

import com.lpavs.caliinda.feature.event_management.messages.FunMessagesImpl
import com.lpavs.caliinda.feature.event_management.messages.IFunMessages
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EventManagementModule {

  @Binds @Singleton abstract fun bindFunMessages(impl: FunMessagesImpl): IFunMessages
}
