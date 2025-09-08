package com.lpavs.caliinda.core.data.di

import com.lpavs.caliinda.feature.agent.data.AgentRepository
import com.lpavs.caliinda.feature.agent.data.AgentRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AgentModule {
  @Binds @Singleton abstract fun bindAgentRepository(impl: AgentRepositoryImpl): AgentRepository
}
