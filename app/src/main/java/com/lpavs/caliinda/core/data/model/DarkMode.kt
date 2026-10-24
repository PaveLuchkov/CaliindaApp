package com.lpavs.caliinda.core.data.model

import android.app.UiModeManager

/** Светлая или тёмная тема приложения — независимо от цветовой темы (ThemeMode). */
enum class DarkMode(val uiMode: Int) {
  SYSTEM(UiModeManager.MODE_NIGHT_AUTO),
  LIGHT(UiModeManager.MODE_NIGHT_NO),
  DARK(UiModeManager.MODE_NIGHT_YES),
}
