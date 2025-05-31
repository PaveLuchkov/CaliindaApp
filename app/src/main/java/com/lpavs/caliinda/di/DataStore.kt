package com.lpavs.caliinda.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

// Объявляем DataStore как расширение Context
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> {
        return appContext.settingsDataStore
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BackendUrl

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WebClientId