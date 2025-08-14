package com.lpavs.caliinda.feature.agent.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import com.lpavs.caliinda.feature.agent.presentation.vm.RecordingState
import com.lpavs.caliinda.feature.calendar.ui.CalendarState
import kotlinx.coroutines.launch

@Composable
fun RecordButton(
    calendarState: CalendarState,
    recordState: RecordingState,
    onStartRecording: () -> Unit, // Принимаем лямбды вместо ViewModel
    onStopRecordingAndSend: () -> Unit,
    onUpdatePermissionResult: (Boolean) -> Unit,
    modifier: Modifier = Modifier // Принимаем внешний модификатор
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isPressed by remember { mutableStateOf(false) } // Для UI эффекта нажатия

  // --- Анимации и формы (без изменений) ---
  val targetBackgroundColor =
      if (recordState.isListening) {
        colorScheme.error
      } else {
        colorScheme.primary
      }
  val animatedBackgroundColor by
      animateColorAsState(
          targetValue = targetBackgroundColor,
          animationSpec = tween(durationMillis = 300),
          label = "RecordButtonBgColor")
  val animatedContentColor = contentColorFor(animatedBackgroundColor)

  // --- Определение форм (предполагаем, что Morph, RoundedPolygon, CornerRounding в util) ---
  val shapeA = remember { RoundedPolygon(numVertices = 5, rounding = CornerRounding(0.5f)) }
  val shapeB = remember { RoundedPolygon.star(9, rounding = CornerRounding(0.3f), radius = 4f) }
  val morph = remember { Morph(shapeA, shapeB) }

  val infiniteTransition = rememberInfiniteTransition("morph_transition")
  val animatedProgress =
      infiniteTransition.animateFloat(
          initialValue = 0f,
          targetValue = 1f,
          animationSpec =
              infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
          label = "animatedMorphProgress")
  val animatedRotation =
      infiniteTransition.animateFloat(
          initialValue = 0f,
          targetValue = 360f,
          animationSpec =
              infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
          label = "animatedMorphRotation")
  val animatedScale by
      animateFloatAsState(
          targetValue = if (isPressed || recordState.isListening) 1.45f else 1.0f,
          animationSpec = tween(durationMillis = 300),
          label = "RecordButtonScale")

  // Лаунчер для запроса разрешения
  val requestPermissionLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
          isGranted: Boolean ->
        onUpdatePermissionResult(isGranted) // Вызываем лямбду для обновления состояния в ViewModel
        if (!isGranted) {
          Toast.makeText(context, "Разрешение на запись отклонено.", Toast.LENGTH_LONG).show()
        } else {
          Log.i("RecordButton", "Permission granted by user.")
          Toast.makeText(
                  context,
                  "Разрешение получено. Нажмите и удерживайте для записи.",
                  Toast.LENGTH_SHORT)
              .show()
        }
      }

  // Кнопка активна, если пользователь вошел и не идет загрузка/запись (для начала записи)
  // Сама логика pointerInput будет обрабатывать uiState.isListening для остановки
  val isInteractionEnabled = calendarState.isSignedIn && !recordState.isLoading

  Log.d(
      "RecordButton",
      "Rendering FAB: isInteractionEnabled=$isInteractionEnabled, isPressed=$isPressed, isListening=${recordState.isListening}")
  FloatingActionButton(
      onClick = {
        // Если нужно простое нажатие для запроса разрешения, если его нет
        if (!recordState.isPermissionGranted && isInteractionEnabled) {
          scope.launch { requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        }
        Log.d("RecordButton", "FAB onClick triggered (handled permission request if needed)")
      },
      containerColor = animatedBackgroundColor,
      contentColor = animatedContentColor,
      modifier =
          modifier
              //                .fillMaxSize()
              .pointerInput(
                  isInteractionEnabled,
                  recordState.isPermissionGranted) { // Передаем зависимости в key
                    if (!isInteractionEnabled) {
                      Log.d("RecordButton", "Interaction disabled, returning from pointerInput.")
                      return@pointerInput // Не обрабатываем ввод, если кнопка неактивна
                    }
                    awaitPointerEventScope {
                      while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        Log.d("RecordButton", "Pointer down detected.")
                        isPressed = true
                        try {
                          // Проверяем разрешение прямо перед началом записи
                          val hasPermission =
                              ContextCompat.checkSelfPermission(
                                  context, Manifest.permission.RECORD_AUDIO) ==
                                  PackageManager.PERMISSION_GRANTED

                          // Обновляем статус разрешения в ViewModel на всякий случай
                          onUpdatePermissionResult(hasPermission)

                          if (hasPermission) {
                            // Если уже идет запись, нажатие игнорируем (остановка по отпусканию)
                            if (!recordState.isListening) {
                              Log.d("RecordButton", "Permission granted, starting recording.")
                              down.consume() // Потребляем событие
                              scope.launch { onStartRecording() } // Вызываем лямбду начала записи
                              try {
                                waitForUpOrCancellation() // Ждем отпускания
                                Log.d("RecordButton", "Pointer up detected, stopping recording.")
                              } finally {
                                // Всегда останавливаем запись при отпускании/отмене, если она была
                                // начата
                                scope.launch {
                                  onStopRecordingAndSend()
                                } // Вызываем лямбду остановки
                              }
                            } else {
                              Log.d("RecordButton", "Already recording, waiting for up.")
                              // Потребляем событие, чтобы оно не всплыло
                              down.consume()
                              // Просто ждем отпускания, чтобы остановить запись (логика в finally)
                              try {
                                waitForUpOrCancellation()
                                Log.d(
                                    "RecordButton",
                                    "Pointer up detected while recording, stopping recording.")
                              } finally {
                                // Всегда останавливаем запись при отпускании/отмене
                                scope.launch { onStopRecordingAndSend() }
                              }
                            }
                          } else {
                            Log.d("RecordButton", "Permission denied, requesting permission.")
                            down.consume() // Потребляем событие, чтобы не начать запись
                            scope.launch {
                              requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                            try {
                              waitForUpOrCancellation() // Ждем отпускания (ничего не делаем)
                              Log.d("RecordButton", "Pointer up after permission request.")
                            } finally {
                              /* Ничего не делаем */
                            }
                          }
                        } finally {
                          Log.d(
                              "RecordButton",
                              "Pointer input block finished, setting isPressed=false.")
                          isPressed = false // Сбрасываем состояние нажатия
                        }
                      }
                    }
                  }
              .clip(
                  if (isPressed || recordState.isListening) {
                    // Используем CustomRotatingMorphShape из папки common
                    CustomRotatingMorphShape(
                        morph = morph,
                        percentage = animatedProgress.value,
                        rotation = animatedRotation.value)
                  } else {
                    // Стандартная форма FAB обычно CircleShape, но оставим RoundedCornerShape, как
                    // было
                    FloatingActionButtonDefaults.shape
                    //                        MaterialShapes.Cookie4Sided.toShape() // Используем
                    // стандартную форму FAB
                  })
              .graphicsLayer { // Масштабируем и вращаем весь Box
                scaleX = animatedScale
                scaleY = animatedScale
              },
  ) {
    Icon(
        imageVector = Icons.Filled.Mic,
        contentDescription =
            if (recordState.isListening) "Идет запись (Отпустите для остановки)"
            else "Начать запись (Нажмите и удерживайте)",
        tint = animatedContentColor)
  }
}
