package com.lpavs.caliinda.app

import android.app.Application
import com.lpavs.caliinda.feature.calendar.ui.components.FunMessages
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CaliindaApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    FunMessages.resetSession()
  }
}
