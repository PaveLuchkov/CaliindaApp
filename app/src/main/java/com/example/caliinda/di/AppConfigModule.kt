package com.example.caliinda.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.caliinda.BuildConfig

@Module
@InstallIn(SingletonComponent::class) // Предоставляем как синглтоны на уровне приложения
object AppConfigModule {

    @Provides
    @Singleton // Эти значения не меняются во время работы приложения
    @BackendUrl // Используем квалификатор
    fun provideBackendBaseUrl(): String {
        return BuildConfig.BACKEND_BASE_URL
    }

    @Provides
    @Singleton
    @WebClientId // Используем квалификатор
    fun provideWebClientId(): String {
        // Убедитесь, что вы запросили serverAuthCode и idToken с правильным ID
        return BuildConfig.BACKEND_WEB_CLIENT_ID
    }
}