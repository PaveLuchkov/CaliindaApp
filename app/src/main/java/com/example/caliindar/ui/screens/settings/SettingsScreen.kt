package com.example.caliindar.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.caliindar.ui.screens.main.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel, // <--- Принимаем ViewModel как параметр
    onSignInClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // Используем переданный viewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp) // Дополнительные отступы для контента
                .fillMaxWidth() // Занимаем всю ширину
        ) {
            if (uiState.isLoading && !uiState.isSignedIn) {
                // Показываем индикатор во время процесса входа
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (!uiState.isSignedIn) {
                Button(
                    onClick = onSignInClick, // Вызываем лямбду
                    enabled = !uiState.isLoading // Блокируем кнопку во время входа
                ) {
                    Text("Войти через Google")
                }
            } else {
                Text("Вы вошли как: ${uiState.userEmail ?: "..."}")
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.signOut() },
                    enabled = !uiState.isLoading
                ) {
                    Text("Выйти")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // TODO: Добавить сюда выбор таймзоны или другие настройки
            Text("Другие настройки будут здесь.")

        }
    }
}