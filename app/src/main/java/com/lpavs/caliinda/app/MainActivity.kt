package com.lpavs.caliinda.app

// Android Framework Imports

// Android Resources

// AndroidX Imports
// AndroidX Compose

// Accompanist

// Google APIs and Services

// Kotlin Coroutines

// Other Libraries

// Project-Specific Imports
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.utils.parseIcs
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.ui.Modifier
import com.lpavs.caliinda.core.ui.theme.CaliindaTheme
import com.lpavs.caliinda.feature.settings.vm.SettingsViewModel
import com.lpavs.caliinda.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  private val settingsViewModel: SettingsViewModel by viewModels()
  @Inject lateinit var settingsRepository: SettingsRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    // После пересоздания интент тот же — не открываем форму второй раз.
    if (savedInstanceState == null) handleIntent(intent)

    setContent {
      val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
      val vibrant by settingsViewModel.vibrantColors.collectAsStateWithLifecycle()
      CaliindaTheme(themeMode = themeMode, vibrant = vibrant) {
        AppNavHost(
            modifier = Modifier.background(colorScheme.background),
        )
      }
    }
  }

  // singleTop: нажатие на виджет при открытом приложении приходит сюда, а не новой копией.
  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent) {
    val uri = intent.data
    if (intent.action == Intent.ACTION_VIEW && uri != null && isIcs(intent, uri)) importIcs(uri)
    else LaunchRequests.handle(intent)
  }

  // Почта отдаёт вложение то с типом text/calendar, то application/octet-stream — смотрим и имя.
  private fun isIcs(intent: Intent, uri: Uri): Boolean =
      intent.type?.substringBefore(';')?.trim() in ICS_TYPES ||
          contentResolver.getType(uri) in ICS_TYPES ||
          uri.lastPathSegment?.endsWith(".ics", ignoreCase = true) == true

  /** .ics из почты или файлов: читаем вне главного потока и открываем форму с его полями. */
  private fun importIcs(uri: Uri) {
    lifecycleScope.launch {
      // Время без пояса — в поясе из настроек, как и всё в приложении.
      val zone = settingsRepository.zoneFlow.first()
      val event =
          withContext(Dispatchers.IO) {
            runCatching {
                  contentResolver.openInputStream(uri)?.use { stream ->
                    // Приглашение — пара килобайт; огромный файл не тащим в память целиком.
                    val bytes = stream.readNBytesCompat(MAX_ICS_BYTES)
                    parseIcs(String(bytes, Charsets.UTF_8), zone)
                  }
                }
                .getOrNull()
          }
      if (event == null) {
        Toast.makeText(this@MainActivity, R.string.ics_import_failed, Toast.LENGTH_SHORT).show()
        return@launch
      }
      LaunchRequests.requestInsert(
          InsertEventRequest(
              title = event.summary,
              description = event.description,
              location = event.location,
              beginMillis = event.beginMillis,
              endMillis = event.endMillis,
              isAllDay = event.isAllDay))
    }
  }

  private fun InputStream.readNBytesCompat(limit: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    while (out.size() < limit) {
      val n = read(buffer, 0, minOf(buffer.size, limit - out.size()))
      if (n < 0) break
      out.write(buffer, 0, n)
    }
    return out.toByteArray()
  }

  private companion object {
    val ICS_TYPES = setOf("text/calendar", "text/x-vcalendar", "application/ics")
    const val MAX_ICS_BYTES = 1024 * 1024
  }
}
