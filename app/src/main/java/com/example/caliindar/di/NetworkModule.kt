package com.example.caliindar.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                // Уровень логирования можно вынести в BuildConfig
                // if (BuildConfig.DEBUG) {
                level = HttpLoggingInterceptor.Level.BODY
                // } else {
                //    level = HttpLoggingInterceptor.Level.NONE
                // }
            })
            .build()
    }

    // TODO: Сюда же можно добавить @Provides для Retrofit, Gson и т.д., если они понадобятся
}