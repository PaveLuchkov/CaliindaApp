package com.lpavs.caliinda.feature.calendar.presentation.components.page

import com.lpavs.caliinda.feature.event_management.EventActions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.IntroState
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalendarPagerScreen(
    /** Состояние страницы дня. Flow на дату создаётся один раз — см. [rememberPageState]. */
    dayPageState: (LocalDate) -> Flow<DayPageUiState>,
    monthPageState: (YearMonth) -> Flow<MonthPageUiState>,
    actions: EventActions,
    onIntroNext: () -> Unit,
    calendarPagerState: PagerState,
    dailyViewPagerState: PagerState,
    monthPagerState: PagerState,
    anchorMonth: YearMonth,
    hasCalendarAccess: Boolean,
    onGrantAccessClick: () -> Unit,
    createEventAction: () -> Unit,
    initialPageIndex: Int,
    anchorDate: LocalDate,
    introductionState: IntroState
) {

    HorizontalPager(
      state = calendarPagerState,
      modifier = Modifier.fillMaxSize(),
      userScrollEnabled = hasCalendarAccess
    ) { page ->
        when (page) {
          0 ->
              // Вниз — следующие месяцы, вверх — история.
              VerticalPager(
                  state = monthPagerState,
                  modifier = Modifier.fillMaxSize(),
                  key = { index ->
                    anchorMonth.plusMonths((index - initialPageIndex).toLong()).toString()
                  },
                  flingBehavior =
                      PagerDefaults.flingBehavior(
                          state = monthPagerState, snapPositionalThreshold = 0.05f),
                  userScrollEnabled = hasCalendarAccess,
                  beyondViewportPageCount = 1) { pageIndex ->
                    val month =
                        remember(pageIndex) {
                          anchorMonth.plusMonths((pageIndex - initialPageIndex).toLong())
                        }
                    MonthProjectsPage(
                        pageState = rememberPageState(month, MonthPageUiState(), monthPageState),
                        actions = actions,
                        hasCalendarAccess = hasCalendarAccess,
                        onGrantAccessClick = onGrantAccessClick,
                        createEventClick = createEventAction,
                        introductionState = introductionState)
                  }

          1 ->
              VerticalPager(
                  state = dailyViewPagerState,
                  modifier = Modifier.fillMaxSize(),
                  key = { index ->
                    anchorDate.plusDays((index - initialPageIndex).toLong()).toEpochDay()
                  },
                  flingBehavior =
                      PagerDefaults.flingBehavior(
                          state = dailyViewPagerState, snapPositionalThreshold = 0.05f),
                  userScrollEnabled = hasCalendarAccess,
                  beyondViewportPageCount = 1) { pageIndex ->
                    val pageDate =
                        remember(pageIndex) {
                          anchorDate.plusDays((pageIndex - initialPageIndex).toLong())
                        }
                    // Сдвиг пейджера относительно этой страницы в px — для «веса» карточек при
                    // перелистывании. Считаем от pageIndex: абсолютный номер страницы огромный,
                    // и во Float мелкие сдвиги потерялись бы.
                    val pagePosition =
                        remember(pageIndex) {
                          {
                            val info = dailyViewPagerState.layoutInfo
                            (dailyViewPagerState.currentPage - pageIndex +
                                dailyViewPagerState.currentPageOffsetFraction) *
                                (info.pageSize + info.pageSpacing)
                          }
                        }
                    DayEventsPage(
                        pageState =
                            rememberPageState(pageDate, DayPageUiState(isLoading = true), dayPageState),
                        actions = actions,
                        // Пока пейджер листается, жест должен достаться ему, а не списку
                        // страницы — иначе они "дерутся" и пейджер застревает между днями.
                        listScrollEnabled = !dailyViewPagerState.isScrollInProgress,
                        pagePosition = pagePosition,
                        hasCalendarAccess = hasCalendarAccess,
                        onGrantAccessClick = onGrantAccessClick,
                        createEventClick = createEventAction,
                        introductionState = introductionState,
                        onIntroNext = onIntroNext)
                  }
        }
      }
}

/** Flow создаём один раз на ключ: иначе каждая рекомпозиция перезапускала бы запрос к календарю. */
@Composable
private fun <K, S> rememberPageState(key: K, initial: S, flowFor: (K) -> Flow<S>): S {
  val flow = remember(key) { flowFor(key) }
  return flow.collectAsStateWithLifecycle(initialValue = initial).value
}
