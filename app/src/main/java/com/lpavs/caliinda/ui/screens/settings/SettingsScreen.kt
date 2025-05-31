package com.lpavs.caliinda.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.ui.screens.main.MainViewModel
import androidx.compose.material.icons.rounded.AccessTimeFilled
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lpavs.caliinda.R


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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
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
                        contentDescription = stringResource(R.string.ai)
                    )
                },
                title = stringResource(R.string.aisettings),
                onClick = onNavigateToAISettings
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsItem(
                icon = {
                    Icon(
                        Icons.Rounded.AccessTimeFilled,
                        tint = colorScheme.onPrimaryContainer,
                        contentDescription = stringResource(R.string.time)
                    )
                },
                title = stringResource(R.string.time_format),
                onClick = onNavigateToTimeSettings
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsItem(
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.doc),
                        tint = colorScheme.onPrimaryContainer,
                        contentDescription = stringResource(R.string.terms)
                    )
                },
                title = stringResource(R.string.terms_of_use),
                onClick = onNavigateToTermsOfuse
            )
        }
    }
}
