package com.lpavs.caliinda.feature.agent.presentation.input

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
import com.lpavs.caliinda.feature.calendar.presentation.CalendarState
import kotlinx.coroutines.launch

@Composable
fun RecordButton(
    calendarState: CalendarState,
    recordState: RecordingState,
    onStartRecording: () -> Unit,
    onStopRecordingAndSend: () -> Unit,
    onUpdatePermissionResult: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isPressed by remember { mutableStateOf(false) }

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

  val requestPermissionLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
          isGranted: Boolean ->
        onUpdatePermissionResult(isGranted)
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

  val isInteractionEnabled = calendarState.isSignedIn && !recordState.isLoading

  Log.d(
      "RecordButton",
      "Render: enabled=$isInteractionEnabled, pressed=$isPressed, listening=${recordState.isListening}, loading=${recordState.isLoading}")

  FloatingActionButton(
      onClick = {
        // Только обрабатываем разрешения при простом клике
        Log.d("RecordButton", "FAB onClick - handling permissions only")
      },
      containerColor = animatedBackgroundColor,
      contentColor = animatedContentColor,
      modifier =
          modifier
              .pointerInput(isInteractionEnabled) {
                awaitPointerEventScope {
                  while (true) {
                    if (!isInteractionEnabled) {
                      Log.d("RecordButton", "Interaction disabled, skipping gesture handling")
                      //                            delay(100) // Небольшая задержка чтобы не
                      // нагружать CPU
                      continue
                    }

                    // Ждем нажатие
                    val down = awaitFirstDown(requireUnconsumed = false)
                    Log.d("RecordButton", "👇 Pointer DOWN")

                    isPressed = true

                    try {
                      // Проверяем разрешения
                      val hasPermission =
                          ContextCompat.checkSelfPermission(
                              context, Manifest.permission.RECORD_AUDIO) ==
                              PackageManager.PERMISSION_GRANTED

                      onUpdatePermissionResult(hasPermission)

                      if (!hasPermission) {
                        Log.d("RecordButton", "❌ No permission, requesting...")
                        down.consume()
                        scope.launch {
                          requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                        waitForUpOrCancellation()
                        Log.d("RecordButton", "👆 Released after permission request")
                        continue
                      }

                      // Есть разрешения - начинаем запись
                      Log.d("RecordButton", "🎙️ Starting recording")
                      down.consume()

                      // Запускаем запись в корутине
                      val recordingJob = scope.launch { onStartRecording() }

                      // Ждем отпускание кнопки
                      val upEvent = waitForUpOrCancellation()
                      Log.d("RecordButton", "👆 Pointer UP - stopping recording")

                      // Останавливаем запись
                      scope.launch { onStopRecordingAndSend() }
                    } catch (e: Exception) {
                      Log.e("RecordButton", "❌ Error in gesture handling", e)
                    } finally {
                      isPressed = false
                      Log.d("RecordButton", "🔄 Reset isPressed = false")
                    }
                  }
                }
              }
              .clip(
                  if (isPressed || recordState.isListening) {
                    CustomRotatingMorphShape(
                        morph = morph,
                        percentage = animatedProgress.value,
                        rotation = animatedRotation.value)
                  } else {
                    FloatingActionButtonDefaults.shape
                  })
              .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
              },
  ) {
    Icon(
        imageVector = Icons.Filled.Mic,
        contentDescription =
            if (recordState.isListening) {
              "Идет запись (Отпустите для остановки)"
            } else {
              "Начать запись (Нажмите и удерживайте)"
            },
        tint = animatedContentColor)
  }
}
