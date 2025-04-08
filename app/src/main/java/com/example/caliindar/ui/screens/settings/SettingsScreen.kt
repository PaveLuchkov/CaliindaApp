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
    onSignInClick: () -> Unit, // Принимаем лямбду из MainActivity/NavHost
    onNavigateBack: () -> Unit // Лямбда для возврата назад
) {
    val viewModel: MainViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Показываем ошибки аутентификации, которые могли произойти при входе
    LaunchedEffect(uiState.showAuthError) {
        uiState.showAuthError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearAuthError() // Очищаем ошибку после показа
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
                    enabled = !uiState.isLoading // Блокируем кнопку во время выхода
                ) {
                    Text("Выйти")
                }
                if (uiState.isLoading) { // Показываем индикатор во время выхода
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // TODO: Добавить сюда выбор таймзоны или другие настройки
            Text("Другие настройки (например, таймзона) будут здесь.")

        }
    }
}