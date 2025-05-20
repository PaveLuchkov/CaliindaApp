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
    // Используем переданный viewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    // Показываем ошибки аутентификации
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
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp) // Дополнительные отступы для контента
                .fillMaxWidth() // Занимаем всю ширину
        ) {
            if (uiState.isLoading && !uiState.isSignedIn) {
                // Показываем индикатор во время процесса входа
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
                        contentDescription = "AI"
                    )
                },
                title = "AI Settings",
                onClick = onNavigateToAISettings
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsItem(
                icon = {
                    Icon(
                        Icons.Rounded.AccessTimeFilled,
                        tint = colorScheme.onPrimaryContainer,
                        contentDescription = "Time"
                    )
                },
                title = "Time & Format",
                onClick = onNavigateToTimeSettings
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsItem(
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.doc),
                        tint = colorScheme.onPrimaryContainer,
                        contentDescription = "Terms"
                    )
                },
                title = "Terms of Use",
                onClick = onNavigateToTermsOfuse
            )
        }
    }
}
