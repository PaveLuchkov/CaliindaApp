package com.example.caliindar.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.caliindar.ui.screens.main.MainViewModel
import kotlinx.coroutines.launch
import java.time.ZoneId
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.input.key.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onSignInClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // Используем переданный viewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentTemper by viewModel.botTemperState.collectAsStateWithLifecycle()
    var temperInputState by remember(currentTemper) { mutableStateOf(currentTemper) }
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
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Настройки Поведения AI",
                style = typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = temperInputState,
                onValueChange = { temperInputState = it },
                label = { Text("Задайте поведение") },
                placeholder = { Text("Например: 'Отвечай кратко и по делу'") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5, // Позволим вводить несколько строк
                // Опционально: обработка клавиатуры (например, убрать фокус при Done)
                keyboardOptions = KeyboardOptions( imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                    }
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // Сохраняем только если значение изменилось
                    if (temperInputState != currentTemper) {
                        viewModel.updateBotTemperSetting(temperInputState)
                        scope.launch {
                            snackbarHostState.showSnackbar("Настройка поведения сохранена")
                        }
                        // Можно добавить скрытие клавиатуры здесь
                    }
                },
                // Кнопка активна, если текст в поле не совпадает с сохраненным
                enabled = temperInputState != currentTemper
            ) {
                Text("Сохранить поведение")
            }


            Spacer(modifier = Modifier.height(24.dp))
            Text("Часовой пояс: ${ZoneId.systemDefault().id} (Как на устройстве)") // Показываем текущий

            Spacer(modifier = Modifier.height(24.dp))
            Text("Другие настройки будут здесь.")

        }
    }
}
