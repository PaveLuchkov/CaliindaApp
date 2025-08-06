package com.lpavs.caliinda.app

import android.app.Application
import com.lpavs.caliinda.feature.calendar.ui.components.IFunMessages
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CaliindaApplication : Application() {
  @Inject lateinit var funMessages: IFunMessages

  override fun onCreate() {
    super.onCreate()
    funMessages.resetSession()
  }
}
