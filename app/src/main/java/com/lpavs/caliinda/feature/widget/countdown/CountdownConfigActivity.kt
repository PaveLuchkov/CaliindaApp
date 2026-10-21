package com.lpavs.caliinda.feature.widget.countdown

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.toShape
import androidx.compose.ui.platform.LocalConfiguration
import com.lpavs.caliinda.core.ui.theme.CaliindaFonts
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.feature.calendar.presentation.model.dateRange
import java.time.YearMonth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
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
import com.lpavs.caliinda.feature.settings.ui.settingsShapes
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
      val vibrant by settingsRepository.vibrantColorsFlow.collectAsStateWithLifecycle(false)
      val zone by settingsRepository.zoneFlow.collectAsStateWithLifecycle(ZoneId.systemDefault())
      val today = remember(zone) { LocalDate.now(zone) }
      val events by
          remember(today) { calendarRepository.getUpcomingFlow(today, COUNTDOWN_LOOKAHEAD_DAYS) }
              .collectAsStateWithLifecycle(null)
      val scope = rememberCoroutineScope()
      CaliindaTheme(themeMode = themeMode, vibrant = vibrant) {
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
    // Виджет могли убрать с рабочего стола, пока открыт выбор, — тогда просто закрываемся.
    val glanceId =
        runCatching { GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId) }.getOrNull()
            ?: return finish()
    updateAppWidgetState(this, PreferencesGlanceStateDefinition, glanceId) { prefs ->
      prefs.toMutablePreferences().apply {
        // У экземпляра серии храним мастер-событие: следующий год найдётся по нему.
        this[COUNTDOWN_EVENT_ID] = event.countdownKey
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
  val searchState = rememberSearchBarState()
  val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
  val now = remember { Instant.now() }
  val q = query.text.trim()
  // По месяцам: так проще найти «тот самый» отпуск или день рождения. Уже идущие — в текущем.
  val groups =
      remember(events, q) {
        events
            ?.filter { it.endTime.isAfter(now) }
            ?.filter { q.isEmpty() || it.displayTitle(context).contains(q, ignoreCase = true) }
            ?.groupBy { YearMonth.from(maxOf(it.startTime.atZone(zone).toLocalDate(), today)) }
      }

  Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      containerColor = colorScheme.surface,
      topBar = {
        AppBarWithSearch(
            state = searchState,
            inputField = {
              SearchBarDefaults.InputField(
                  textFieldState = query,
                  searchBarState = searchState,
                  onSearch = {},
                  placeholder = { Text(stringResource(R.string.countdown_search)) },
                  leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) })
            },
            navigationIcon = {
              IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.back))
              }
            },
            scrollBehavior = scrollBehavior)
      }) { padding ->
        when {
          groups == null ->
              Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingIndicator()
              }
          else ->
              LazyColumn(
                  contentPadding = padding,
                  modifier = Modifier.fillMaxSize().padding(horizontal = cuid.ItemHorizontalPadding)) {
                    item {
                      Column(Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                        Text(
                            stringResource(R.string.countdown_config_title),
                            style = typography.headlineMediumEmphasized,
                            fontFamily = CaliindaFonts.Heavy)
                        Text(
                            stringResource(R.string.countdown_config_subtitle),
                            style = typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant)
                      }
                    }
                    if (groups.isEmpty()) {
                      item {
                        Text(
                            stringResource(R.string.countdown_config_empty),
                            style = typography.bodyLarge,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(32.dp))
                      }
                    }
                    groups.forEach { (month, monthEvents) ->
                      item(key = month.toString()) { MonthHeader(month, today) }
                      itemsIndexed(monthEvents, key = { _, it -> it.id }) { index, event ->
                        EventRow(
                            event = event,
                            today = today,
                            zone = zone,
                            index = index,
                            count = monthEvents.size,
                            onClick = { onPick(event) })
                        Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
                      }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                  }
        }
      }
}

@Composable
private fun MonthHeader(month: YearMonth, today: LocalDate) {
  val locale = LocalConfiguration.current.locales[0]
  // Год — только если не текущий: «Октябрь», но «Январь 2027».
  val pattern = if (month.year == today.year) "LLLL" else "LLLL yyyy"
  Text(
      text = DateTimeFormatter.ofPattern(pattern, locale).format(month).replaceFirstChar {
        it.titlecase(locale)
      },
      style = typography.titleSmall,
      color = colorScheme.secondary,
      modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 8.dp))
}

/** Событие в пикере: фигура с числом дней — та же, что будет в виджете. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EventRow(
    event: EventDto,
    today: LocalDate,
    zone: ZoneId,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
  val locale = LocalConfiguration.current.locales[0]
  val title = event.displayTitle()
  val range = event.dateRange(zone)
  val days = ChronoUnit.DAYS.between(today, range.start).toInt()
  val dateFormat = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }
  val dates =
      if (range.start == range.endInclusive) dateFormat.format(range.start)
      else "${dateFormat.format(range.start)} – ${dateFormat.format(range.endInclusive)}"
  val shape = countdownPolygon(event.countdownKey).toShape()

  SegmentedListItem(
      onClick = onClick,
      shapes = settingsShapes(index = index, count = count),
      colors = ListItemDefaults.segmentedColors(containerColor = colorScheme.surfaceContainer),
      leadingContent = {
        Box(
            modifier = Modifier.size(52.dp).background(colorScheme.tertiary, shape),
            contentAlignment = Alignment.Center) {
              Text(
                  // Сегодня и уже идущие: считать нечего, ставим эмодзи из названия.
                  text = if (days > 0) days.toString() else leadingEmoji(title) ?: "⏳",
                  // Три цифры в 52dp мелким кеглем, иначе упираются в зубцы.
                  style = if (days > 99) typography.labelLarge else typography.titleMedium,
                  fontFamily = CaliindaFonts.Heavy,
                  color = colorScheme.onTertiary)
            }
      },
      supportingContent = {
        Text(
            listOf(
                    dates,
                    if (days > 0) pluralStringResource(R.plurals.countdown_in_days, days, days)
                    else stringResource(R.string.countdown_today))
                .joinToString(" · "),
            maxLines = 1)
      }) {
        Text(title, fontFamily = CaliindaFonts.Card, style = typography.titleMedium, maxLines = 2)
      }
}
