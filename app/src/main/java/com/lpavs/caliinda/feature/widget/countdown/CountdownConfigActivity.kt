package com.lpavs.caliinda.feature.widget.countdown

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.repository.CalendarRepository
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import com.lpavs.caliinda.core.ui.theme.CaliindaTheme
import com.lpavs.caliinda.core.data.model.ThemeMode
import com.lpavs.caliinda.core.ui.util.displayTitle
import com.lpavs.caliinda.core.ui.util.projectShape
import com.lpavs.caliinda.feature.settings.ui.settingsShapes
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Выбор события для виджета обратного отсчёта. Система открывает его при добавлении виджета и из
 * его настроек; пока не выбрали — виджет не добавляется (RESULT_CANCELED).
 */
@AndroidEntryPoint
class CountdownConfigActivity : ComponentActivity() {
  @Inject lateinit var calendarRepository: CalendarRepository
  @Inject lateinit var settingsRepository: SettingsRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    val appWidgetId =
        intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    setResult(RESULT_CANCELED, resultIntent(appWidgetId))
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      finish()
      return
    }

    setContent {
      val themeMode by settingsRepository.themeModeFlow.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
      val zone by settingsRepository.zoneFlow.collectAsStateWithLifecycle(ZoneId.systemDefault())
      val today = remember(zone) { LocalDate.now(zone) }
      val events by
          remember(today) { calendarRepository.getUpcomingFlow(today, COUNTDOWN_LOOKAHEAD_DAYS) }
              .collectAsStateWithLifecycle(null)
      val scope = rememberCoroutineScope()
      CaliindaTheme(themeMode = themeMode) {
        CountdownPicker(
            events = events,
            today = today,
            zone = zone,
            onPick = { event -> scope.launch { pick(appWidgetId, event) } },
            onClose = ::finish)
      }
    }
  }

  private suspend fun pick(appWidgetId: Int, event: EventDto) {
    val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)
    updateAppWidgetState(this, PreferencesGlanceStateDefinition, glanceId) { prefs ->
      prefs.toMutablePreferences().apply {
        // У экземпляра серии храним мастер-событие: следующий год найдётся по нему.
        this[COUNTDOWN_EVENT_ID] = event.originalEventId ?: event.eventId
        this[COUNTDOWN_TITLE] = event.displayTitle(this@CountdownConfigActivity)
      } as Preferences
    }
    CountdownWidget().update(this, glanceId)
    setResult(RESULT_OK, resultIntent(appWidgetId))
    finish()
  }

  private fun resultIntent(appWidgetId: Int) =
      Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CountdownPicker(
    events: List<EventDto>?,
    today: LocalDate,
    zone: ZoneId,
    onPick: (EventDto) -> Unit,
    onClose: () -> Unit,
) {
  val context = LocalContext.current
  val query = rememberTextFieldState()
  val now = remember { Instant.now() }
  val visible =
      events
          ?.filter { it.endTime.isAfter(now) }
          ?.filter {
            val q = query.text.trim()
            q.isEmpty() || it.displayTitle(context).contains(q, ignoreCase = true)
          }
  val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      containerColor = colorScheme.surface,
      topBar = {
        LargeFlexibleTopAppBar(
            title = { Text(stringResource(R.string.countdown_config_title)) },
            subtitle = { Text(stringResource(R.string.countdown_config_subtitle)) },
            navigationIcon = {
              IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.back))
              }
            },
            scrollBehavior = scrollBehavior)
      }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
          OutlinedTextField(
              state = query,
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
              placeholder = { Text(stringResource(R.string.countdown_search)) },
              leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
              lineLimits = TextFieldLineLimits.SingleLine,
              shape = ListItemDefaults.shapes().shape)
          when {
            visible == null -> Unit
            visible.isEmpty() ->
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                  Text(
                      stringResource(R.string.countdown_config_empty),
                      style = typography.bodyLarge,
                      color = colorScheme.onSurfaceVariant,
                      textAlign = TextAlign.Center)
                }
            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                      itemsIndexed(visible, key = { _, it -> it.id }) { index, event ->
                        val start = event.startTime.atZone(zone).toLocalDate()
                        val days = ChronoUnit.DAYS.between(today, start).toInt()
                        SegmentedListItem(
                            onClick = { onPick(event) },
                            shapes = settingsShapes(index = index, count = visible.size),
                            colors =
                                ListItemDefaults.segmentedColors(
                                    containerColor = colorScheme.surfaceContainer),
                            leadingContent = {
                              // Фигура события, как она будет в виджете.
                              Box(
                                  modifier =
                                      Modifier.size(40.dp)
                                          .background(colorScheme.tertiary, projectShape(event.id)),
                                  contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (days > 0) days.toString() else "•",
                                        style = typography.labelLarge,
                                        color = colorScheme.onTertiary)
                                  }
                            },
                            supportingContent = {
                              Text(
                                  listOf(
                                          dateFormat.format(start),
                                          when {
                                            days <= 0 ->
                                                stringResource(R.string.countdown_today)
                                            else ->
                                                pluralStringResource(
                                                    R.plurals.countdown_in_days, days, days)
                                          })
                                      .joinToString(" · "),
                                  maxLines = 1)
                            }) {
                              Text(event.displayTitle(), maxLines = 2)
                            }
                      }
                    }
          }
        }
      }
}
