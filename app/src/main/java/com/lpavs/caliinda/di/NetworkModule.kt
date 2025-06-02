package com.lpavs.caliinda.di

import android.content.Context
import androidx.room.Room
import com.lpavs.caliinda.data.local.AppDatabase
import com.lpavs.caliinda.data.local.EventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

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

@Module
@InstallIn(SingletonComponent::class) // Живет пока живет приложение
object DatabaseModule {

  @Provides
  @Singleton // Гарантирует один экземпляр базы данных
  fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
    return Room.databaseBuilder(
            appContext, AppDatabase::class.java, "caliindar_database" // Имя файла БД
            )
        // ВНИМАНИЕ: Для разработки можно использовать .fallbackToDestructiveMigration()
        // Но для production нужно реализовать правильные миграции!
        .fallbackToDestructiveMigration()
        .build()
  }

  @Provides
  @Singleton // DAO тоже должен быть синглтоном, т.к. зависит от синглтона БД
  fun provideEventDao(appDatabase: AppDatabase): EventDao {
    return appDatabase.eventDao()
  }
}
