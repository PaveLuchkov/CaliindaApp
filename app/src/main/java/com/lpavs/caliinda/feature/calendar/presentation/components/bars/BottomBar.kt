package com.lpavs.caliinda.feature.calendar.presentation.components.bars

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.auth.AuthState
import com.lpavs.caliinda.feature.agent.presentation.input.RecordButton
import com.lpavs.caliinda.feature.agent.presentation.input.SuggestionChipsRow
import com.lpavs.caliinda.feature.agent.presentation.vm.RecordingState
import com.lpavs.caliinda.feature.calendar.presentation.CalendarState

@ExperimentalMaterial3ExpressiveApi
@Composable
fun BottomBar(
    calendarState: CalendarState,
    authState: AuthState,
    recordState: RecordingState,
    textFieldValue: TextFieldValue,
    onTextChanged: (TextFieldValue) -> Unit,
    onSendClick: () -> Unit,
    onRecordStart: () -> Unit,
    onRecordStopAndSend: () -> Unit,
    onUpdatePermissionResult: (Boolean) -> Unit,
    isTextInputVisible: Boolean,
    modifier: Modifier = Modifier,
    onCreateEventClick: () -> Unit,
    suggestions: List<String>? = emptyList(),
) {
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current
  val isSendEnabled =
      textFieldValue.text.isNotBlank() &&
          authState.isSignedIn &&
          !recordState.isLoading &&
          !recordState.isListening
  var expanded by rememberSaveable { mutableStateOf(true) }
  var onKeyboardToggle by remember { mutableStateOf(true) }

  LaunchedEffect(isTextInputVisible) {
    if (isTextInputVisible) {
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
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    if (!suggestions.isNullOrEmpty()) {
      Box() {
        SuggestionChipsRow(
            suggestions,
            enabled = !recordState.isLoading,
            onChipClick = { suggestionText -> onTextChanged(TextFieldValue(suggestionText)) })
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
    AnimatedContent(
        modifier = modifier,
        targetState = onKeyboardToggle,
        transitionSpec = {
          val fadeSpringSpec =
              spring<Float>(
                  dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
          val sizeTransformSpringSpec =
              spring<IntSize>(
                  dampingRatio = Spring.DampingRatioLowBouncy,
                  stiffness = Spring.StiffnessMediumLow)
          if (targetState) {
                (fadeIn(animationSpec = fadeSpringSpec)).togetherWith(
                    fadeOut(animationSpec = fadeSpringSpec))
              } else {
                (fadeIn(animationSpec = fadeSpringSpec)).togetherWith(
                    fadeOut(animationSpec = fadeSpringSpec))
              }
              .using(
                  SizeTransform(
                      clip = false, sizeAnimationSpec = { _, _ -> sizeTransformSpringSpec }))
        }) {
          if (!it) {
            HorizontalFloatingToolbar(
                expanded = expanded,
                floatingActionButton = {
                  FloatingActionButton(
                      onClick = onSendClick,
                      contentColor = colorScheme.onPrimary,
                      containerColor = colorScheme.primary) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Отправить")
                      }
                },
                content = {
                  IconButton(
                      onClick = { onKeyboardToggle = !onKeyboardToggle },
                  ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Убрать ввод текста")
                  }
                  OutlinedTextField(
                      value = textFieldValue,
                      onValueChange = onTextChanged,
                      modifier = Modifier.width(200.dp),
                      placeholder = { Text(stringResource(R.string.type_message)) },
                      maxLines = 1,
                      keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                      keyboardActions =
                          KeyboardActions(
                              onSend = {
                                if (isSendEnabled) {
                                  onSendClick()
                                }
                              }),
                      colors =
                          OutlinedTextFieldDefaults.colors(
                              focusedBorderColor = Color.Transparent,
                              unfocusedBorderColor = Color.Transparent,
                          ),
                      singleLine = true,
                  )
                },
            )
          } else {
            HorizontalFloatingToolbar(
                expanded = expanded,
                floatingActionButton = {
                  RecordButton(
                      calendarState = calendarState,
                      onStartRecording = onRecordStart,
                      onStopRecordingAndSend = onRecordStopAndSend,
                      onUpdatePermissionResult = onUpdatePermissionResult,
                      recordState = recordState)
                },
                content = {
                  IconButton(
                      onClick = onCreateEventClick,
                  ) {
                    Icon(imageVector = Icons.Filled.AddCircle, contentDescription = "Create event")
                  }
                  IconButton(
                      onClick = { onKeyboardToggle = !onKeyboardToggle },
                  ) {
                    Icon(
                        imageVector = Icons.Filled.Keyboard,
                        contentDescription = "Показать клавиатуру")
                  }
                },
            )
          }
        }
  }
}
