package com.lpavs.caliinda.app

import android.app.Application
import com.lpavs.caliinda.feature.event_management.messages.IFunMessages
import com.lpavs.caliinda.feature.live.LiveEventNotifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CaliindaApplication : Application() {
  @Inject lateinit var funMessages: IFunMessages
  @Inject lateinit var liveEventNotifier: LiveEventNotifier

  override fun onCreate() {
    super.onCreate()
    funMessages.resetSession()
    // Будильник мог потеряться (принудительная остановка) — восстанавливаем при каждом старте.
    liveEventNotifier.refreshAsync()
  }
}
