package com.lpavs.caliinda.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CaliindaApplication : Application() {
  override fun onCreate() {
    super.onCreate()
  }
}
