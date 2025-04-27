package com.example.caliindar.ui.screens.main.components

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ChatInputBar(
    uiState: com.example.caliindar.ui.screens.main.MainUiState, // Принимаем весь стейт
    textFieldValue: TextFieldValue,
    onTextChanged: (TextFieldValue) -> Unit,
    onSendClick: () -> Unit,
    onRecordStart: () -> Unit, // Лямбда для начала записи
    onRecordStopAndSend: () -> Unit, // Лямбда для остановки/отправки
    onUpdatePermissionResult: (Boolean) -> Unit, // Лямбда для обновления разрешения
    isTextInputVisible: Boolean,
    onToggleTextInput: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isSendEnabled = textFieldValue.text.isNotBlank() && uiState.isSignedIn && !uiState.isLoading && !uiState.isListening
    val isKeyboardToggleEnabled = uiState.isSignedIn && !uiState.isLoading && !uiState.isListening // Disable toggle during recording/loading

    // Request focus when text input becomes visible
    LaunchedEffect(isTextInputVisible) {
        if (isTextInputVisible) {
            // kotlinx.coroutines.delay(100) // Small delay might be needed if focus doesn't work immediately
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
                Log.d("ChatInputBar", "Focus requested and keyboard show attempted.")
            } catch (e: Exception) {
                Log.e("ChatInputBar", "Error requesting focus or showing keyboard", e)
            }
        } else {
            keyboardController?.hide()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- Text Input Row (Conditionally Visible) ---
        AnimatedVisibility(
            visible = isTextInputVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), // Slide in from bottom + Fade in
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut() // Slide out to bottom + Fade out
        ) {
            Surface(tonalElevation = 4.dp) { // Add elevation similar to input fields
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = onTextChanged,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester), // Apply focus requester
                        shape = RoundedCornerShape(24.dp), // More rounded
                        placeholder = { Text("Сообщение...") },
                        enabled = uiState.isSignedIn && !uiState.isLoading && !uiState.isListening,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors( // Optional: Customize colors
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Send Button (IconButton)
                    IconButton(
                        onClick = onSendClick,
                        enabled = isSendEnabled,
                        modifier = Modifier.size(48.dp), // Consistent size
                        colors = IconButtonDefaults.iconButtonColors(
                            // Используем цвета M3
                            containerColor = if (isSendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, // Используем surfaceVariant для disabled
                            contentColor = if (isSendEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f) // Стандартный disabled alpha
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Отправить сообщение"
                        )
                    }
                }
            }
        }

        // --- Bottom App Bar with Icons ---
        BottomAppBar(
            // containerColor = MaterialTheme.colorScheme.surface, // Можно задать цвет фона
            // contentColor = MaterialTheme.colorScheme.onSurface, // Цвет контента по умолчанию
            // TODO: ACTION кнопка создания события
            actions = {
                // Keyboard Toggle Button
                IconButton(
                    onClick = onToggleTextInput,
                    enabled = isKeyboardToggleEnabled
                ) {
                    Icon(
                        imageVector = Icons.Filled.Keyboard,
                        contentDescription = if (isTextInputVisible) "Скрыть клавиатуру" else "Показать клавиатуру"
                    )
                }
                // You could add other actions here if needed
            },
            floatingActionButton = {
                // Используем отдельный компонент RecordButton
                RecordButton(
                    uiState = uiState, // Передаем стейт
                    onStartRecording = onRecordStart, // Передаем лямбды
                    onStopRecordingAndSend = onRecordStopAndSend,
                    onUpdatePermissionResult = onUpdatePermissionResult,
                    modifier = Modifier.size(56.dp) // Standard FAB size
                )
            },
            // floatingActionButtonPosition = FabPosition.Center // Позиционирование FAB
        )
    }
}