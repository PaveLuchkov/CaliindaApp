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
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween // Для настройки скорости анимации
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.lazy.LazyListState // Убедитесь, что импорт есть
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch // Для запуска корутины сброса скролла
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetDemo() {
    val sheetState = rememberModalBottomSheetState() // Можно добавить skipPartiallyExpanded = true, если нужно
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Show bottom sheet") },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Show bottom sheet") },
                onClick = {
                    showBottomSheet = true
                }
            )
        }
    ) { contentPadding ->
        // Screen content - Замените Box на ваше реальное содержимое экрана
        Box(
            modifier = Modifier
                .fillMaxSize() // Пример: занять все место
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("Main Screen Content")
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = sheetState
            ) {
                // Sheet content - Замените на ваше реальное содержимое шторки
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("This is the Bottom Sheet", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        scope.launch {
                            sheetState.hide()
                            showBottomSheet = false
                        }
                    }) {
                        Text("Hide bottom sheet")
                    }
                    Spacer(modifier = Modifier.height(32.dp)) // Добавим немного места снизу
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomSheetPreview() { // Изменил имя Preview функции для ясности
    MaterialTheme {
        BottomSheetDemo()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetDemo2() {

    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var skipPartiallyExpanded by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bottomSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)

// App content
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                Modifier.toggleable(
                    value = skipPartiallyExpanded,
                    role = Role.Checkbox,
                    onValueChange = { checked: Boolean -> skipPartiallyExpanded = checked }
                )
            ) {
                Checkbox(checked = skipPartiallyExpanded, onCheckedChange = null)
                Spacer(Modifier.width(16.dp))
                Text("Skip partially expanded State")
            }
            Button(
                onClick = { openBottomSheet = !openBottomSheet },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Show Bottom Sheet")
            }
        }
    }

// Sheet content
    if (openBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { openBottomSheet = false },
            sheetState = bottomSheetState,
            contentWindowInsets = { WindowInsets.navigationBars } // Recommended for system nav bar
        ) {
            // Use a Box to layer the scrollable content and the fixed button row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(bottom = 88.dp) // ADJUST THIS VALUE AS NEEDED
                ) {
                    // 1a. Поле ввода текста
                    var text by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        label = { Text("Text field") }
                    )

                    // 1b. Прокручиваемый список занимает все доступное оставшееся место в этой Column
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f) // Takes remaining space in *this* Column
                            .fillMaxWidth()
                    ) {
                        items(25) {
                            ListItem(
                                headlineContent = { Text("Item $it") },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = "Localized description"
                                    )
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                // 2. Фиксированный Row с кнопкой внизу Box
                Row(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter) // Align this Row to the bottom of the Box
                        .background(MaterialTheme.colorScheme.surface) // Add background to cover scrolled content
                        .padding(horizontal = 16.dp, vertical = 16.dp), // Padding for the content within the Row
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            scope
                                .launch { bottomSheetState.hide() }
                                .invokeOnCompletion {
                                    if (!bottomSheetState.isVisible) {
                                        openBottomSheet = false
                                    }
                                }
                        }
                    ) {
                        Text("Hide Bottom Sheet")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomSheetPreview2() { // Изменил имя Preview функции для ясности
    MaterialTheme {
        BottomSheetDemo2()
    }
}