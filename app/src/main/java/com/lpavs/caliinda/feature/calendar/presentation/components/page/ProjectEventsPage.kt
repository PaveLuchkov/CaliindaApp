package com.lpavs.caliinda.feature.calendar.presentation.components.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.feature.calendar.presentation.CalendarViewModel
import java.time.LocalDate


@Composable
fun ProjectEventsPage(
    isLoading: Boolean,
    displayPosition: WeekState,
    viewModel: CalendarViewModel,
) {
    val pageState by
    viewModel
        .getDayPageUiState(LocalDate.now())
        .collectAsStateWithLifecycle(initialValue = DayPageUiState(isLoading = true))
}