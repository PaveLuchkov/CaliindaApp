package com.lpavs.caliinda.core.data.di

import android.content.Context
import androidx.room.Room
import com.lpavs.caliinda.core.data.local.AppDatabase
import com.lpavs.caliinda.core.data.local.EventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
        .addInterceptor(
            HttpLoggingInterceptor().apply {
              level = HttpLoggingInterceptor.Level.BODY
            })
        .build()
  }

  // TODO: Сюда же можно добавить @Provides для Retrofit, Gson и т.д., если они понадобятся
}

@Module
@InstallIn(SingletonComponent::class) // Живет пока живет приложение
object DatabaseModule {

  @Provides
  @Singleton // Гарантирует один экземпляр базы данных
  fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
    return Room.databaseBuilder(
        appContext, AppDatabase::class.java, "caliindar_database"
    )
        // ВНИМАНИЕ: Для разработки можно использовать .fallbackToDestructiveMigration()
        // Но для production нужно реализовать правильные миграции!
        .fallbackToDestructiveMigration(false)
        .build()
  }

  @Provides
  @Singleton // DAO тоже должен быть синглтоном, т.к. зависит от синглтона БД
  fun provideEventDao(appDatabase: AppDatabase): EventDao {
    return appDatabase.eventDao()
  }
}
