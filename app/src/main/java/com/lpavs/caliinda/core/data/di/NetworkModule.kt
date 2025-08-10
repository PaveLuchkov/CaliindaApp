package com.lpavs.caliinda.core.data.di

import android.content.Context
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.lpavs.caliinda.core.data.auth.AuthApiService
import com.lpavs.caliinda.core.data.local.AppDatabase
import com.lpavs.caliinda.core.data.local.CalendarLocalDataSource
import com.lpavs.caliinda.core.data.remote.CalendarApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

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
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()
  }

  @Provides
  @Singleton
  fun provideRetrofit(okHttpClient: OkHttpClient, @BackendUrl baseUrl: String): Retrofit {
    val json = Json { ignoreUnknownKeys = true }
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
  }

  @Provides
  @Singleton
  fun provideCalendarApiService(retrofit: Retrofit): CalendarApiService {
    return retrofit.create(CalendarApiService::class.java)
  }

  @Provides
  @Singleton
  fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
    return retrofit.create(AuthApiService::class.java)
  }
}

@Module
@InstallIn(SingletonComponent::class) // Живет пока живет приложение
object DatabaseModule {

  @Provides
  @Singleton // Гарантирует один экземпляр базы данных
  fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
    return Room.databaseBuilder(appContext, AppDatabase::class.java, "caliindar_database")
        // ВНИМАНИЕ: Для разработки можно использовать .fallbackToDestructiveMigration()
        // Но для production нужно реализовать правильные миграции!
        .fallbackToDestructiveMigration(false)
        .build()
  }

  @Provides
  @Singleton // DAO тоже должен быть синглтоном, т.к. зависит от синглтона БД
  fun provideEventDao(appDatabase: AppDatabase): CalendarLocalDataSource {
    return appDatabase.eventDao()
  }
}
