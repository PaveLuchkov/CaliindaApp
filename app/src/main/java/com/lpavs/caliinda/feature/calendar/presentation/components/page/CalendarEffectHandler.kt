package com.lpavs.caliinda.feature.calendar.presentation.components.page

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.core.data.auth.AuthViewModel
import com.lpavs.caliinda.feature.agent.presentation.vm.AgentUiEvent
import com.lpavs.caliinda.feature.agent.presentation.vm.AgentViewModel
import com.lpavs.caliinda.feature.calendar.presentation.CalendarUiEvent
import com.lpavs.caliinda.feature.calendar.presentation.CalendarViewModel
import com.lpavs.caliinda.feature.event_management.vm.EventManagementUiEvent
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import kotlinx.coroutines.flow.merge
import java.time.LocalDate

@Composable
fun CalendarEffectHandler(
    calendarViewModel: CalendarViewModel,
    eventManagementViewModel: EventManagementViewModel,
    agentViewModel: AgentViewModel,
    authViewModel: AuthViewModel,
    pagerState: PagerState,
    snackbarHostState: SnackbarHostState,
    context: Context = LocalContext.current,
    initialPageIndex: Int,
    today: LocalDate,
    authorizationLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>
) {
  val authState by authViewModel.authState.collectAsStateWithLifecycle()
  LaunchedEffect(authState.authorizationIntent) {
    authState.authorizationIntent?.let { pendingIntent ->
      try {
        val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent).build()
        authorizationLauncher.launch(intentSenderRequest)
        authViewModel.clearAuthorizationIntent()
      } catch (e: Exception) {
        Log.e("CalendarEffect", "Couldn't start authorization UI", e)
      }
    }
  }

  LaunchedEffect(pagerState.targetPage) {
    val settledDate = today.plusDays((pagerState.targetPage - initialPageIndex).toLong())
    calendarViewModel.onVisibleDateChanged(settledDate)
  }

  LaunchedEffect(Unit) {
    merge(
        agentViewModel.eventFlow,
        calendarViewModel.eventFlow,
        eventManagementViewModel.eventFlow)
        .collect { event ->
          when (event) {
            is AgentUiEvent.ShowMessage -> {
              snackbarHostState.showSnackbar(event.message.asString(context))
            }
            is CalendarUiEvent.ShowMessage -> {
              snackbarHostState.showSnackbar(event.message)
            }
            is EventManagementUiEvent.ShowMessage -> {
              snackbarHostState.showSnackbar(event.message.asString(context))
            }
            is EventManagementUiEvent.OperationSuccess -> {}
          }
        }
  }

  LaunchedEffect(Unit) {
    val hasPermission =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    agentViewModel.updatePermissionStatus(hasPermission)
  }
}
