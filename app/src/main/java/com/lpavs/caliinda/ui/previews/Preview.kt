import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import kotlin.math.max
import kotlin.random.Random
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import java.time.LocalDateTime
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.* // Используем Material3
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.TextButton

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import java.time.LocalDate

import android.util.Log
import kotlin.math.abs

import androidx.compose.animation.* // Импорты для AnimatedContent и анимаций
import androidx.compose.animation.core.EaseInExpo
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.tween // Для настройки скорости анимации
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.lazy.LazyListState // Убедитесь, что импорт есть
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch // Для запуска корутины сброса скролла
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.pointer.* // <<< Импорт для PointerEvent, PointerId и т.д.

import androidx.compose.runtime.rememberCoroutineScope // Добавить импорт
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import androidx.compose.ui.text.input.TextFieldValue

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalAnimationApi::class)
@Composable
fun Bar(
    textFieldValue: TextFieldValue,
    onTextChanged: (TextFieldValue) -> Unit,
) {
    var onKeyboardToggle by remember { mutableStateOf(true)}
    val vibrantColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
    HorizontalFloatingToolbar(
        expanded = true,
        floatingActionButton = {
            AnimatedContent(
                targetState = onKeyboardToggle,
                transitionSpec = {
                    // Анимация для FAB: простой fade in/out
                    // Если onKeyboardToggle = true (показываем микрофон), то он въезжает, Send выезжает.
                    // Если onKeyboardToggle = false (показываем Send), то он въезжает, микрофон выезжает.
                    if (targetState) { // Переход к микрофону
                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> -height } + fadeOut())
                    } else { // Переход к кнопке Send
                        (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> height } + fadeOut())
                    }
                },
                label = "fab_animation"
            ){isIconMode ->
                if (!isIconMode) {
                    FloatingActionButton(
                        onClick = {},
                    ){
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Отправить",
                        )
                    }
                }
                else
                    FloatingActionButton(
                        onClick = {},
                    ){
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Голосовое",
                        )
                    }
            }
        },
        expandedShadowElevation = 0.dp,
        colors = vibrantColors,
        content = {
            AnimatedContent(
                targetState = onKeyboardToggle,
                label = "content animation"
            ) {isInputMode ->
                if (!isInputMode) {
                    IconButton(
                        onClick = {onKeyboardToggle = !onKeyboardToggle},
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Убрать ввод текста"
                        )
                    }
                    OutlinedTextField( // Или TextField, или BasicTextField + кастомное оформление
                        value = textFieldValue,
                        onValueChange = onTextChanged,
                        modifier = Modifier.width(200.dp),
                        placeholder = { Text("Type message") },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = colorScheme.onSecondaryContainer,
                        ),
                        singleLine = true,
                    )}
                else {
                    IconButton(
                        onClick = {
                        },
                        // enabled = isKeyboardToggleEnabled TODO : enable after done
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = "Create event"
                        )
                    }
                    IconButton(
                        onClick = { onKeyboardToggle = !onKeyboardToggle },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Keyboard,
                            contentDescription = "Показать клавиатуру"
                        )
                    }
                }
            }

        },
    )
}

@OptIn(ExperimentalAnimationApi::class) // Для AnimatedContent
@Composable
fun ToggleBetweenComponents() {
    var textFieldState by remember { mutableStateOf(TextFieldValue("")) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Кнопка для переключения
        Bar(
            textFieldValue = textFieldState,
            onTextChanged = { textFieldState = it }
            )
    }
}

@Preview(showBackground = true)
@Composable
fun ToggleComponentsPreview() {
    MaterialTheme {
        Box(modifier = Modifier
            .fillMaxSize()
            , contentAlignment = Alignment.Center) {
            ToggleBetweenComponents()
        }
    }
}