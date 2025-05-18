package com.lpavs.caliinda.ui.screens.main.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.lpavs.caliinda.ui.screens.main.MainViewModel
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable


@ExperimentalMaterial3ExpressiveApi
@Composable
fun BottomBar(
    uiState: com.lpavs.caliinda.ui.screens.main.MainUiState, // Принимаем весь стейт
    textFieldValue: TextFieldValue,
    onTextChanged: (TextFieldValue) -> Unit,
    onSendClick: () -> Unit,
    onRecordStart: () -> Unit, // Лямбда для начала записи
    onRecordStopAndSend: () -> Unit, // Лямбда для остановки/отправки
    onUpdatePermissionResult: (Boolean) -> Unit, // Лямбда для обновления разрешения
    isTextInputVisible: Boolean,
    onToggleTextInput: () -> Unit,
    viewModel: MainViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isSendEnabled = textFieldValue.text.isNotBlank() && uiState.isSignedIn && !uiState.isLoading && !uiState.isListening
    val isKeyboardToggleEnabled = uiState.isSignedIn && !uiState.isListening // Disable toggle during recording/loading
    var expanded by rememberSaveable { mutableStateOf(true) }
    val vibrantColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()

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
    HorizontalFloatingToolbar(
        expanded = expanded,
        floatingActionButton = {
            RecordButton(
                uiState = uiState, // Передаем стейт
                onStartRecording = onRecordStart, // Передаем лямбды
                onStopRecordingAndSend = onRecordStopAndSend,
                onUpdatePermissionResult = onUpdatePermissionResult,
                modifier = Modifier.size(56.dp) // Standard FAB size
            )
        },
        modifier =
            modifier, //.align(Alignment.BottomEnd) .offset(x = -ScreenOffset, y = -ScreenOffset)
        colors = vibrantColors,
        content = {
            IconButton(
                onClick = {
                    val selectedDate = viewModel.currentVisibleDate.value // Берем видимую дату
                    navController.navigate("create_event/${selectedDate.toEpochDay()}")
                },
                // enabled = isKeyboardToggleEnabled TODO : enable after done
            ) {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = "Create event"
                )
            }
            IconButton(
                onClick = onToggleTextInput,
                enabled = isKeyboardToggleEnabled
            ) {
                Icon(
                    imageVector = Icons.Filled.Keyboard,
                    contentDescription = if (isTextInputVisible) "Скрыть клавиатуру" else "Показать клавиатуру"
                )
            }
        },
    )
}