package com.lpavs.caliinda.ui.screens.main.components

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomBar(
    uiState: com.lpavs.caliinda.ui.screens.main.MainUiState,
    textFieldValue: TextFieldValue,
    onTextChanged: (TextFieldValue) -> Unit,
    onSendClick: () -> Unit,
    onRecordStart: () -> Unit,
    onRecordStopAndSend: () -> Unit,
    onUpdatePermissionResult: (Boolean) -> Unit,
    isTextInputVisible: Boolean,
    onToggleTextInput: () -> Unit,
    viewModel: MainViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isSendEnabled = textFieldValue.text.isNotBlank() && uiState.isSignedIn && !uiState.isLoading && !uiState.isListening
    val isKeyboardToggleEnabled = uiState.isSignedIn && !uiState.isListening

    // Request focus when text input becomes visible
    LaunchedEffect(isTextInputVisible) {
        if (isTextInputVisible) {
            // Небольшая задержка может помочь, если фокус не срабатывает сразу
            // Особенно важно после смены UI (Crossfade)
            delay(50) // Можно увеличить до 100-200мс если нужно
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
                Log.d("BottomBar", "Focus requested and keyboard show attempted.")
            } catch (e: Exception) {
                Log.e("BottomBar", "Error requesting focus or showing keyboard", e)
            }
        } else {
            keyboardController?.hide()
            Log.d("BottomBar", "Keyboard hide attempted.")
        }
    }

    Crossfade(
        targetState = isTextInputVisible,
        animationSpec = tween(durationMillis = 200), // Настройте скорость анимации
        label = "ToolbarToInputCrossfade",
        modifier = modifier // Применяем общий модификатор к Crossfade
    ) { inputVisible ->
        if (inputVisible) {
            ChatInputTextField(
                textFieldValue = textFieldValue,
                onTextChanged = onTextChanged,
                onSendClick = onSendClick,
                focusRequester = focusRequester,
                isSendEnabled = isSendEnabled,
                onCloseInput = onToggleTextInput, // Эта функция теперь будет скрывать текстовое поле
                // modifier = Modifier.fillMaxWidth() // Модификатор для этого конкретного состояния
            )
        } else {
            val vibrantColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
            HorizontalFloatingToolbar(
                expanded = true, // Тулбар всегда "развернут", когда виден
                floatingActionButton = {
                    RecordButton(
                        uiState = uiState,
                        onStartRecording = onRecordStart,
                        onStopRecordingAndSend = onRecordStopAndSend,
                        onUpdatePermissionResult = onUpdatePermissionResult,
                    )
                },
                expandedShadowElevation = 0.dp,
                // modifier = modifier, // Модификатор уже применен к Crossfade
                colors = vibrantColors,
                content = {
                    IconButton(
                        onClick = {
                            val selectedDate = viewModel.currentVisibleDate.value
                            navController.navigate("create_event/${selectedDate.toEpochDay()}")
                        },
                        // enabled = isKeyboardToggleEnabled // TODO: решить, нужна ли эта проверка здесь
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = "Create event"
                        )
                    }
                    IconButton(
                        onClick = onToggleTextInput, // Эта функция теперь будет ПОКАЗЫВАТЬ текстовое поле
                        enabled = isKeyboardToggleEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Keyboard,
                            contentDescription = "Показать клавиатуру"
                        )
                    }
                },
            )
        }
    }
}

@Composable
fun ChatInputTextField(
    textFieldValue: TextFieldValue,
    onTextChanged: (TextFieldValue) -> Unit,
    onSendClick: () -> Unit,
    focusRequester: FocusRequester,
    isSendEnabled: Boolean,
    onCloseInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp) // Добавляем немного отступов
            .navigationBarsPadding(), // Чтобы не перекрывалось системной навигацией
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCloseInput) {
            Icon(
                imageVector = Icons.Filled.KeyboardHide, // или ArrowBack
                contentDescription = "Скрыть клавиатуру и показать тулбар"
            )
        }

        OutlinedTextField( // Или TextField, или BasicTextField + кастомное оформление
            value = textFieldValue,
            onValueChange = onTextChanged,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = { Text("Введите сообщение...") },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (isSendEnabled) {
                        onSendClick()
                    }
                }
            ),
            singleLine = true, // или maxLines, если нужно многострочное
            // colors = TextFieldDefaults.outlinedTextFieldColors(...) // Настройте цвета при необходимости
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSendClick,
            enabled = isSendEnabled
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "Отправить",
                tint = if (isSendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    }
}