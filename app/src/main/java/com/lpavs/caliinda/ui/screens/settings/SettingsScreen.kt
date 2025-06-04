package com.lpavs.caliinda.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AccessTimeFilled
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.ui.screens.main.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onSignInClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToAISettings: () -> Unit,
    onNavigateToTimeSettings: () -> Unit,
    onNavigateToTermsOfuse: () -> Unit
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.showAuthError) {
    uiState.showAuthError?.let { error ->
      snackbarHostState.showSnackbar(error)
      viewModel.clearAuthError()
    }
  }
  Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.settings)) },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back))
              }
            })
      }) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(paddingValues).padding(16.dp).fillMaxWidth()) {
              if (uiState.isLoading && !uiState.isSignedIn) {
                LoadingIndicator()
                Spacer(modifier = Modifier.height(16.dp))
              }
              GoogleAccountSection(
                  viewModel = viewModel,
                  onSignInClick = onSignInClick,
              )

              Spacer(modifier = Modifier.height(10.dp))

              SettingsItem(
                  icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ar_sticker),
                        tint = colorScheme.onPrimaryContainer,
                        contentDescription = stringResource(R.string.ai))
                  },
                  title = stringResource(R.string.aisettings),
                  onClick = onNavigateToAISettings,
            shape = MaterialShapes.Clover4Leaf.toShape())

              Spacer(modifier = Modifier.height(10.dp))

              SettingsItem(
                  icon = {
                    Icon(
                        Icons.Rounded.AccessTimeFilled,
                        tint = colorScheme.onPrimaryContainer,
                        contentDescription = stringResource(R.string.time))
                  },
                  title = stringResource(R.string.time_format),
                  onClick = onNavigateToTimeSettings,
                  shape = MaterialShapes.Pill.toShape())

              Spacer(modifier = Modifier.height(10.dp))

              SettingsItem(
                  icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.doc),
                        tint = colorScheme.onPrimaryContainer,
                        contentDescription = stringResource(R.string.terms))
                  },
                  title = stringResource(R.string.terms_of_use),
                  onClick = onNavigateToTermsOfuse,
                  shape = MaterialShapes.Bun.toShape())
            }
      }
}
