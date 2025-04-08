import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Wallpapers.GREEN_DOMINATED_EXAMPLE
import java.time.LocalDateTime
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.*
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.* // Используем Material3
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.BottomAppBar

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.YearMonth

import android.util.Log
import kotlin.math.abs

import androidx.compose.animation.* // Импорты для AnimatedContent и анимаций
import androidx.compose.animation.core.EaseInExpo
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.tween // Для настройки скорости анимации
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.lazy.LazyListState // Убедитесь, что импорт есть
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion
import kotlinx.coroutines.launch // Для запуска корутины сброса скролла
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture // <<< Импорт
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.* // <<< Импорт для PointerEvent, PointerId и т.д.
import com.google.android.material.bottomappbar.BottomAppBar
import kotlin.math.abs // Для abs()
import kotlin.math.sqrt

import androidx.compose.runtime.rememberCoroutineScope // Добавить импорт
import kotlinx.coroutines.launch // Добавить импорт
import kotlinx.coroutines.delay

// Уровни масштаба
enum class TimeScale {
    Day, Week, Month, Year
}

// Функция для получения следующего, более детального масштаба
fun TimeScale.next(): TimeScale? = when (this) {
    TimeScale.Year -> TimeScale.Month
    TimeScale.Month -> TimeScale.Week
    TimeScale.Week -> TimeScale.Day
    TimeScale.Day -> null // Дальше некуда
}

// Функция для получения предыдущего, более общего масштаба
fun TimeScale.previous(): TimeScale? = when (this) {
    TimeScale.Day -> TimeScale.Week
    TimeScale.Week -> TimeScale.Month
    TimeScale.Month -> TimeScale.Year
    TimeScale.Year -> null // Дальше некуда
}

// Данные события
data class TimelineEvent(
    val id: String, // Уникальный идентификатор
    val title: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val description: String? = null,
    // Можно добавить цвет, иконку и т.д.
    val applicableScale: List<TimeScale> // Указываем, на каких масштабах событие релевантно
)

// Ключ для идентификации состояния таймлайна (для анимаций и derivedStateOf)
@Immutable
data class TimelineKey(val date: LocalDate, val scale: TimeScale)

// --- Оптимизированный Composable Экран ---

@OptIn(ExperimentalAnimationApi::class)
@Preview(showBackground = true, device = "id:pixel_6a")
@Composable
fun CalendarTimelineScreen() {

    // --- Состояния ---
    var currentScale by rememberSaveable { mutableStateOf(TimeScale.Day) }
    var currentDateAnchor by rememberSaveable { mutableStateOf(LocalDate.now()) }
    val listState = rememberLazyListState() // Состояние для LazyColumn

    // Ключ для AnimatedContent, пересчитывается только при изменении даты или масштаба
    val timelineKey by remember {
        derivedStateOf { TimelineKey(currentDateAnchor, currentScale) }
    }

    // --- Константы и Помощники ---
    // Чувствительность зума (можно вынести в настройки)
    val zoomSensitivityFactor = 1.5f
    // Пороги для Zoom OUT (переход к ПРЕДЫДУЩЕМУ, более общему масштабу)
    // Ключ - масштаб, ОТ КОТОРОГО переходим, значение - порог зума (< 1.0)
    val zoomOutThresholds = remember {
        mapOf(
            TimeScale.Day to 0.7f,
            TimeScale.Week to 0.4f,
            TimeScale.Month to 0.2f
            // Year не имеет предыдущего, порог не нужен
        )
    }
    // Пороги для Zoom IN (переход к СЛЕДУЮЩЕМУ, более детальному масштабу)
    // Ключ - масштаб, ОТ КОТОРОГО переходим, значение - порог зума (> 1.0)
    val zoomInThresholds = remember {
        mapOf(
            TimeScale.Year to 2.0f,
            TimeScale.Month to 1.8f,
            TimeScale.Week to 1.6f
            // Day не имеет следующего, порог не нужен
        )
    }
    // Форматтер для заголовка AppBar, зависит от текущего масштаба
    val currentDateFormat = remember(currentScale) {
        when (currentScale) {
            TimeScale.Day -> DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
            TimeScale.Week -> DateTimeFormatter.ofPattern("'Week of' d MMM yyyy") // Начало недели
            TimeScale.Month -> DateTimeFormatter.ofPattern("MMMM yyyy")
            TimeScale.Year -> DateTimeFormatter.ofPattern("yyyy")
        }
    }
    // Порог для навигации через overscroll (можно настроить)
    val overscrollNavigationThreshold = 200f

    // --- Логика получения данных ---
    // Используем derivedStateOf: список событий пересчитывается только при изменении ключа
    val eventsToShow by remember(timelineKey) {
        derivedStateOf {
            Log.d("Timeline", "DerivedState: Fetching events for ${timelineKey.scale} starting ${timelineKey.date}")
            SampleData.getEventsForPeriod(timelineKey.date, timelineKey.scale)
        }
    }

    // --- Функции навигации по времени ---
    // Используем `currentDateAnchor` и `currentScale` напрямую
    val navigateToPreviousPeriod: () -> Unit = {
        currentDateAnchor = when (currentScale) {
            TimeScale.Day -> currentDateAnchor.minusDays(1)
            TimeScale.Week -> currentDateAnchor.minusWeeks(1)
            TimeScale.Month -> currentDateAnchor.minusMonths(1)
            TimeScale.Year -> currentDateAnchor.minusYears(1)
        }
    }
    val navigateToNextPeriod: () -> Unit = {
        currentDateAnchor = when (currentScale) {
            TimeScale.Day -> currentDateAnchor.plusDays(1)
            TimeScale.Week -> currentDateAnchor.plusWeeks(1)
            TimeScale.Month -> currentDateAnchor.plusMonths(1)
            TimeScale.Year -> currentDateAnchor.plusYears(1)
        }
    }

    // --- Nested Scroll Connection для Перехвата Overscroll ---
    val nestedScrollConnection = rememberOverscrollNavigator(
        listState = listState,
        threshold = overscrollNavigationThreshold,
        onNavigatePrevious = navigateToPreviousPeriod,
        onNavigateNext = navigateToNextPeriod
    )

    // --- Обработчик жестов масштабирования ---
    val zoomGestureHandler = Modifier.pointerInput(Unit) {
        detectZoomGestures(
            currentScaleProvider = { currentScale }, // Передаем текущий масштаб
            zoomOutThresholds = zoomOutThresholds,
            zoomInThresholds = zoomInThresholds,
            zoomSensitivity = zoomSensitivityFactor,
            onScaleChanged = { newScale ->
                // Логика при смене масштаба
                currentScale = newScale
                // Корректируем дату-якорь при смене масштаба для лучшего UX
                currentDateAnchor = when (newScale) {
                    TimeScale.Day -> currentDateAnchor // Оставляем тот же день
                    TimeScale.Week -> currentDateAnchor.with(java.time.DayOfWeek.MONDAY) // Начало недели
                    TimeScale.Month -> currentDateAnchor.withDayOfMonth(1) // Начало месяца
                    TimeScale.Year -> currentDateAnchor.withDayOfYear(1) // Начало года
                }
                Log.d("ZoomGesture", "** Scale Changed to: $newScale! Anchor adjusted to: $currentDateAnchor **")
            }
        )
    }

    // --- UI ---
    Scaffold(
        topBar = { CalendarAppBar(title = currentDateAnchor.format(currentDateFormat)) },
        bottomBar = { CalendarBottomBar() },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection) // Применяем перехватчик overscroll
                .then(zoomGestureHandler) // Применяем обработчик зума
        ) {
            // Фон (если нужен)
            BackgroundShapes(MaterialTheme.colorScheme)

            // Анимированное содержимое таймлайна
            AnimatedContent(
                targetState = timelineKey,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val goingBackwards = targetState.date.isBefore(initialState.date)
                    // Определяем направление сдвига (-1 вверх, 1 вниз)
                    val slideDirection = if (goingBackwards) -1 else 1
                    // Длительность анимации (можно немного увеличить для наглядности)
                    val animationDuration = 400 // мс

                    // Анимация для ВХОДЯЩЕГО контента (нового)
                    val enterTransition = slideInVertically(
                        animationSpec = tween(durationMillis = animationDuration, easing = EaseOutExpo) // Более плавная кривая на выходе
                    ) { height -> slideDirection * height / 2 } + // Начинаем сдвиг с середины? Или с полного height? Попробуем height
                            // ) { height -> slideDirection * height } +
                            fadeIn(
                                animationSpec = tween(durationMillis = animationDuration * 2 / 3, delayMillis = animationDuration / 3) // Появляется чуть позже и не так резко
                                // animationSpec = tween(durationMillis = animationDuration) // Или просто fade за всю длительность
                            )

                    // Анимация для ВЫХОДЯЩЕГО контента (старого)
                    val exitTransition = slideOutVertically(
                        animationSpec = tween(durationMillis = animationDuration, easing = EaseInExpo) // Ускорение в конце
                    ) { height -> -slideDirection * height } + // Сдвигается в противоположном направлении
                            fadeOut(
                                animationSpec = tween(durationMillis = animationDuration / 2) // Исчезает быстрее
                            )

                    // Объединяем анимации
                    enterTransition togetherWith exitTransition using SizeTransform(
                        clip = false // Обязательно, чтобы контент мог выезжать за границы
                    )
                }
            ) { key ->
                // ... остальной код внутри AnimatedContent ...

                LaunchedEffect(key) {
                    // НЕБОЛЬШАЯ ЗАДЕРЖКА перед скроллом может помочь
                    // Дает анимации время "начаться" визуально перед рывком скролла
                    delay(50) // 50-100 мс, подбирается экспериментально
                    if (listState.layoutInfo.totalItemsCount > 0 || eventsToShow.isNotEmpty()) {
                        Log.d("TimelineAnimate", "Scrolling to top for key: $key after delay")
                        listState.scrollToItem(0)
                    } else {
                        Log.d("TimelineAnimate", "Skipping scroll for key: $key, list empty.")
                    }
                }

                TimelineContent(
                    events = eventsToShow,
                    currentScale = key.scale,
                    listState = listState,
                    modifier = Modifier.fillMaxSize()
                )

            }
        }
    }
}


// --- Вынесенный и Оптимизированный NestedScrollConnection ---
@Composable
private fun rememberOverscrollNavigator(
    listState: LazyListState,
    threshold: Float,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit
): NestedScrollConnection {
    // Состояния для накопления overscroll
    var accumulatedOverscrollUp by remember { mutableFloatStateOf(0f) }
    var accumulatedOverscrollDown by remember { mutableFloatStateOf(0f) }

    // --- НОВОЕ: Состояние для блокировки повторной навигации ---
    var isNavigationCooldownActive by remember { mutableStateOf(false) }
    // Получаем CoroutineScope для запуска задержки
    val scope = rememberCoroutineScope()
    // Длительность "охлаждения" в миллисекундах (подбирается экспериментально)
    // Должна быть чуть больше времени анимации + время на обновление состояния
    val navigationCooldownMillis = 500L // Например, 500 мс

    // Сброс накопления при скролле в середине (оставляем)
    LaunchedEffect(listState.isScrollInProgress, listState.canScrollBackward, listState.canScrollForward) {
        if (listState.isScrollInProgress && listState.canScrollBackward && listState.canScrollForward) {
            accumulatedOverscrollUp = 0f
            accumulatedOverscrollDown = 0f
        }
        // Также сбрасываем кулдаун, если начали скроллить в середине (на всякий случай)
        // Хотя он должен сбрасываться по таймеру.
        // if (listState.isScrollInProgress) {
        //     isNavigationCooldownActive = false
        // }
    }

    return remember(listState, threshold, onNavigatePrevious, onNavigateNext) { // Добавим isNavigationCooldownActive в ключи remember, если его изменение должно пересоздавать connection (хотя обычно не нужно)
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero

                val deltaY = available.y
                // --- ПРОВЕРКА: Если активен кулдаун, просто поглощаем скролл и выходим ---
                if (isNavigationCooldownActive) {
                    // Поглощаем вертикальный скролл, чтобы список не "прыгал" во время кулдауна
                    Log.d("TimelineScroll", "Cooldown active, consuming scroll: ${available.y}")
                    return Offset(0f, available.y)
                }

                val canScrollBackward = listState.canScrollBackward
                val canScrollForward = listState.canScrollForward

                when {
                    // Тянем ВНИЗ на ВЕРХНЕМ краю
                    deltaY > 0 && !canScrollBackward -> {
                        accumulatedOverscrollUp += deltaY
                        accumulatedOverscrollDown = 0f
                        // Проверяем порог И что кулдаун НЕ активен
                        if (accumulatedOverscrollUp >= threshold && !isNavigationCooldownActive) {
                            Log.d("TimelineScroll", "--> Navigating Previous Period triggered (UP overscroll)")
                            isNavigationCooldownActive = true // <<< Активируем кулдаун
                            onNavigatePrevious()
                            accumulatedOverscrollUp = 0f // Сброс СРАЗУ ПОСЛЕ навигации
                            // Запускаем корутину для снятия кулдауна через N мс
                            scope.launch {
                                delay(navigationCooldownMillis)
                                isNavigationCooldownActive = false
                                Log.d("TimelineScroll", "Cooldown finished for Previous navigation.")
                            }
                        }
                        // Поглощаем скролл в любом случае при оверскролле вверх
                        return Offset(0f, available.y)
                    }

                    // Тянем ВВЕРХ на НИЖНЕМ краю
                    deltaY < 0 && !canScrollForward -> {
                        accumulatedOverscrollDown += abs(deltaY)
                        accumulatedOverscrollUp = 0f
                        // Проверяем порог И что кулдаун НЕ активен
                        if (accumulatedOverscrollDown >= threshold && !isNavigationCooldownActive) {
                            Log.d("TimelineScroll", "--> Navigating Next Period triggered (DOWN overscroll)")
                            isNavigationCooldownActive = true // <<< Активируем кулдаун
                            onNavigateNext()
                            accumulatedOverscrollDown = 0f // Сброс СРАЗУ ПОСЛЕ навигации
                            // Запускаем корутину для снятия кулдауна через N мс
                            scope.launch {
                                delay(navigationCooldownMillis)
                                isNavigationCooldownActive = false
                                Log.d("TimelineScroll", "Cooldown finished for Next navigation.")
                            }
                        }
                        // Поглощаем скролл в любом случае при оверскролле вниз
                        return Offset(0f, available.y)
                    }

                    // Скролл в середине списка или от края
                    else -> {
                        accumulatedOverscrollUp = 0f
                        accumulatedOverscrollDown = 0f
                        // Здесь кулдаун не трогаем, позволяем обычному скроллу работать
                        return Offset.Zero
                    }
                }
            }

            // onPostScroll можно оставить как есть или убрать, т.к. основная логика в onPreScroll
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Можно добавить сброс кулдауна при явном окончании жеста,
                // но таймера обычно достаточно.
                // if (source == NestedScrollSource.Drag && available.y == 0f) {
                //      isNavigationCooldownActive = false // Возможно, преждевременный сброс
                // }
                return Offset.Zero
            }
        }
    }
}


// --- Вынесенный и Оптимизированный Обработчик Жестов Зума ---
private suspend fun PointerInputScope.detectZoomGestures(
    currentScaleProvider: () -> TimeScale, // Функция для получения текущего масштаба
    zoomOutThresholds: Map<TimeScale, Float>,
    zoomInThresholds: Map<TimeScale, Float>,
    zoomSensitivity: Float,
    onScaleChanged: (TimeScale) -> Unit
) {
    awaitEachGesture {
        // Состояние для ТЕКУЩЕГО жеста
        var gestureZoomFactor = 1.0f // Накопленный зум внутри жеста
        var scaleChangedDuringThisGesture = false // Флаг блокировки смены масштаба в рамках одного жеста
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        // Ждем первый палец
        awaitFirstDown(requireUnconsumed = false)

        do {
            val event: PointerEvent = awaitPointerEvent(PointerEventPass.Main)
            val pointers = event.changes.filter { it.pressed }

            if (pointers.size == 2) { // Работаем только с двумя пальцами
                if (!pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = true)
                    val distance = pointers[0].position.getDistanceSquared()

                    // Проверяем, сдвинулся ли хотя бы один палец достаточно далеко
                    val moved = pointers.any {
                        it.position.getDistanceSquared() > touchSlop * touchSlop // Direct access
                    }
                    if (moved) {
                        pastTouchSlop = true
                        // Инициализируем начальный зум после прохождения touch slop
                        gestureZoomFactor = 1.0f
                        Log.d("ZoomGesture", "Touch slop passed.")
                    }
                }

                if (pastTouchSlop) {
                    // Рассчитываем зум относительно предыдущего шага
                    val stepZoom = event.calculateZoom() // Встроенный расчет зума
                    val centroid = event.calculateCentroid(useCurrent = true)

                    // Применяем чувствительность и накапливаем
                    val effectiveStepZoom = 1.0f + (stepZoom - 1.0f) * zoomSensitivity
                    gestureZoomFactor *= effectiveStepZoom
                    gestureZoomFactor = gestureZoomFactor.coerceIn(0.1f, 10f) // Ограничение накопленного зума

                    // Log.d("ZoomGesture", "StepZoom: $stepZoom, AccumZoom: $gestureZoomFactor, Locked: $scaleChangedDuringThisGesture")

                    // Проверяем пороги для смены масштаба, если еще не меняли в этом жесте
                    if (!scaleChangedDuringThisGesture) {
                        val currentScale = currentScaleProvider() // Получаем актуальный масштаб
                        val previousScale = currentScale.previous()
                        val nextScale = currentScale.next()

                        var newScale: TimeScale? = null

                        // Проверка Zoom Out
                        if (previousScale != null && gestureZoomFactor < (zoomOutThresholds[currentScale] ?: 0f)) {
                            Log.d("ZoomGesture", "--> Zoom Out Triggered: $currentScale -> $previousScale (factor: $gestureZoomFactor)")
                            newScale = previousScale
                        }
                        // Проверка Zoom In
                        else if (nextScale != null && gestureZoomFactor > (zoomInThresholds[currentScale] ?: Float.MAX_VALUE)) {
                            Log.d("ZoomGesture", "--> Zoom In Triggered: $currentScale -> $nextScale (factor: $gestureZoomFactor)")
                            newScale = nextScale
                        }

                        if (newScale != null) {
                            onScaleChanged(newScale) // Вызываем колбек для смены масштаба
                            scaleChangedDuringThisGesture = true // Блокируем дальнейшие изменения в этом жесте
                            // Сброс зума в жесте необязателен, т.к. мы просто блокируем дальнейшие изменения
                            // gestureZoomFactor = 1.0f
                        }
                    }

                    // Потребляем изменения, чтобы они не вызвали pan и т.д.
                    event.changes.forEach { if (it.pressed) it.consume() }
                }
            } else {
                // Если количество пальцев изменилось (не 2), сбрасываем touch slop для следующей проверки
                pastTouchSlop = false
            }
        } while (event.changes.any { it.pressed }) // Продолжаем, пока есть нажатые пальцы

        Log.d("ZoomGesture", "--- Gesture End ---")
        // Сброс состояния жеста происходит автоматически благодаря awaitEachGesture
    }
}


// --- Адаптированный TimelineContent ---
@Composable
fun TimelineContent(
    events: List<TimelineEvent>,
    currentScale: TimeScale,
    listState: LazyListState, // Принимаем состояние
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState, // Используем переданное состояние
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 8.dp) // Объединенный вертикальный padding
    ) {
        if (events.isEmpty()) {
            item {
                // Заполнитель на весь экран, если список пуст
                Box(
                    modifier = Modifier
                        .fillParentMaxSize() // Занимает все доступное пространство LazyColumn
                        .padding(16.dp), // Отступ для текста
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No events for this period.", // Более информативно
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant // Чуть менее контрастный цвет
                    )
                }
            }
        } else {
            items(events, key = { it.id }) { event ->
                // Адаптер для выбора нужного вида строки в зависимости от масштаба
                TimelineEventRowAdapter(event = event, currentScale = currentScale)
                // Spacer не нужен здесь, т.к. padding добавлен в Row или Card
            }
        }
    }
}

// --- Адаптер строк ---
@Composable
fun TimelineEventRowAdapter(event: TimelineEvent, currentScale: TimeScale) {
    // Выбираем разное отображение для разных масштабов
    when (currentScale) {
        TimeScale.Day -> DayTimelineEventRow(event = event)
        TimeScale.Week, TimeScale.Month, TimeScale.Year -> GenericTimelineEventRow(event = event, scale = currentScale)
    }
    // Добавляем небольшой отступ между элементами
    Spacer(modifier = Modifier.height(6.dp))
}

// --- Компонент для строки события Дня ---
// Вынесем форматтеры как константы уровня файла для переиспользования
private val timeFormatter = DateTimeFormatter.ofPattern("H:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM")
// private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM H:mm") // Не используется пока

@Composable
fun DayTimelineEventRow(event: TimelineEvent) {
    // Простая проверка на "текущее" событие (пересекает текущий момент)
    val isCurrent = remember(event.startTime, event.endTime) { // Ключи remember должны быть стабильными
        val now = LocalDateTime.now()
        now.isAfter(event.startTime) && now.isBefore(event.endTime)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp), // Небольшой вертикальный отступ для строки
        verticalAlignment = Alignment.Top
    ) {
        // Можно добавить декоративную линию таймлайна слева, если нужно
        // Box(modifier = Modifier.width(2.dp).height(intrinsicSize.height).background(colorScheme.primary))
        Spacer(modifier = Modifier.width(16.dp)) // Отступ слева

        // Используем специализированную карточку для дня
        EventCardDay(
            title = event.title,
            isCurrent = isCurrent,
            startTimeStr = remember(event.startTime) { event.startTime.format(timeFormatter) }, // Форматируем только при изменении
            endTimeStr = remember(event.endTime) { event.endTime.format(timeFormatter) }
        )
    }
}

// --- Компонент для строки события на других масштабах ---
@Composable
fun GenericTimelineEventRow(event: TimelineEvent, scale: TimeScale) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp) // Небольшой вертикальный отступ
            .height(IntrinsicSize.Min), // Высота по содержимому
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp) // Увеличим вертикальный паддинг
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f) // Занимать доступное место
                    .padding(end = 8.dp), // Отступ справа от текста
                maxLines = 1 // Ограничим одной строкой для предсказуемости высоты
            )
            // Показываем даты начала/конца
            Text(
                // Используем remember для форматирования строк
                text = remember(event.startTime, event.endTime) {
                    "${event.startTime.format(dateFormatter)} - ${event.endTime.format(dateFormatter)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}


// --- Верхняя панель приложения ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarAppBar(title: String) {
    CenterAlignedTopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
            IconButton(onClick = { /* TODO: Go to Today */ }) { // Добавим действие
                Icon(Icons.Filled.Today, contentDescription = "Go to Today")
            }
        },
        actions = {
            IconButton(onClick = { /* TODO: Действие для настроек */ }) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        },
        // Добавим цвета для соответствия теме
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = colorScheme.surface, // Или surfaceVariant
            titleContentColor = colorScheme.onSurface,
            navigationIconContentColor = colorScheme.onSurface,
            actionIconContentColor = colorScheme.onSurface
        )
    )
}

// --- Нижняя панель навигации/действий ---
@Composable
fun CalendarBottomBar() {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        actions = {
            IconButton(onClick = { /* TODO: Действие клавиатуры */ }) {
                Icon(Icons.Filled.Keyboard, contentDescription = "Keyboard Input")
            }
            Spacer(Modifier.width(16.dp)) // Add some space between the icons
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Действие микрофона */ },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Mic, contentDescription = "Voice Input")
            }
        }
    )
}


// --- Компонент для карточки события (адаптирован для дня) ---
@Composable
fun EventCardDay(
    title: String,
    isCurrent: Boolean = false,
    startTimeStr: String,
    endTimeStr: String
) {
    // Используем стабильную генерацию формы на основе ID события (если бы он был доступен)
    // или просто одну и ту же форму для консистентности. Убрано рандомное создание.
    // val polygon = remember { RoundedPolygon(...) } // Определить конкретную форму
    // val clip = remember(polygon) { RoundedPolygonShape(polygon = polygon) }
    val star2Shape = remember {
        RoundedPolygon.star(
            3,
            rounding = CornerRounding(0.4f),
        )
    }
    val clip2Star = remember(star2Shape) {
        RoundedPolygonShape(polygon = star2Shape)
    }

    val cardShape = RoundedCornerShape(16.dp) // Стандартная форма

    val backgroundColor by animateColorAsState( // Анимация цвета фона
        targetValue = if (isCurrent) colorScheme.tertiaryContainer else colorScheme.primaryContainer,
        animationSpec = tween(durationMillis = 300)
    )
    val contentColor by animateColorAsState( // Анимация цвета контента
        targetValue = if (isCurrent) colorScheme.onTertiaryContainer else colorScheme.onPrimaryContainer,
        animationSpec = tween(durationMillis = 300)
    )
    val iconBackgroundColor = contentColor // Фон иконки = цвет контента

    Card(
        modifier = Modifier
            .fillMaxWidth() // Занимает доступную ширину в Row
            .height(IntrinsicSize.Min), // Высота по содержимому
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 6.dp else 2.dp) // Чуть меньше тень
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Колонка для времени и разделителя
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.height(IntrinsicSize.Min) // Занимаем высоту строки
            ) {
                Text(
                    text = startTimeStr,
                    fontSize = 10.sp, // Немного увеличим
                    color = contentColor.copy(alpha = 0.8f) // Чуть прозрачнее
                )
                // Простой разделитель вместо сложной фигуры
                Box(
                    modifier = Modifier
                        .padding(vertical = 6.dp) // Больше отступ
                        .clip(clip2Star)
                        .size(width = 10.dp, height = 10.dp) // Вертикальная линия
                        .background(iconBackgroundColor.copy(alpha = 0.5f)) // Полупрозрачный

                )
                Text(
                    text = endTimeStr,
                    fontSize = 10.sp,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp)) // Отступ

            // Текст события
            Text(
                text = title,
                color = contentColor,
                style = MaterialTheme.typography.bodyLarge, // Используем стиль темы
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f) // Занимает оставшееся место
                    .padding(end = 8.dp), // Отступ справа
                maxLines = 2 // Позволим две строки, если не влезает
            )

            // Можно добавить иконку справа, например, для действий
            // Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = contentColor)
        }
    }
}


@Composable
fun BackgroundShapes(colorScheme: ColorScheme) {
    Box(
        modifier = Modifier
            .fillMaxSize()
        // .padding(16.dp)
    ) {
        // Создаем фигуру звезды один раз
        val starShape = remember {
            RoundedPolygon.star(
                17,
                rounding = CornerRounding(0.95f),
            )
        }
        val clipStar = remember(starShape) {
            RoundedPolygonShape(polygon = starShape)
        }
        val star2Shape = remember {
            RoundedPolygon.star(
                4,
                rounding = CornerRounding(0.6f),
            )
        }
        val clip2Star = remember(starShape) {
            RoundedPolygonShape(polygon = star2Shape)
        }
        val starContainerSize = 300.dp
        val star2ContainerSize = 200.dp
        // Холст для отрисовки
        Box(
            modifier = Modifier
                .size(starContainerSize)
                .align(Alignment.TopEnd)
                .offset(
                    x = starContainerSize * 0.2f,
                    y = -starContainerSize * 0.1f
                )
                .clip(clipStar)
                .background(colorScheme.surfaceContainer),
        ) {

        }
        Box(
            modifier = Modifier
                .size(star2ContainerSize)
                .align(Alignment.CenterStart)
                .offset(
                    x = -star2ContainerSize * 0.4f
                )
                .clip(clip2Star)
                .background(colorScheme.surfaceContainer),
        ) {

        }
        Box(
            modifier = Modifier
                .padding(start = 42.dp)
                .fillMaxHeight()
                .width(3.dp)
                .background(colorScheme.scrim)

        )
    }
}

// --- Компонент для карточки события ---
@Composable
fun EventCard(
    title: String,
    isCurrent: Boolean = false,
    startTimeStr: String, // Принимаем строки
    endTimeStr: String
) {

    val polygon = remember { createRandomShape() }
    val clip = remember(polygon) { RoundedPolygonShape(polygon = polygon) }

    // Используем цвета из MaterialTheme.colorScheme
    val backgroundColor = if (isCurrent) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (isCurrent) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
    val iconBackgroundColor = contentColor // Фон иконки делаем таким же, как цвет контента

    Card(
        modifier = Modifier
            .height(IntrinsicSize.Min) // Попробуем высоту по контенту
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 8.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp) // Увеличим вертикальный padding
                .fillMaxWidth(), // Занимаем всю ширину Card
            verticalAlignment = Alignment.CenterVertically // Выравниваем все по центру строки
            // horizontalArrangement = Arrangement.SpaceBetween // Убрали, т.к. используем Spacer и вес
        ) {
            // Колонка для времени и иконки
            Column(
                verticalArrangement = Arrangement.SpaceBetween, // Распределяем время сверху/снизу
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .height(IntrinsicSize.Min)// Занимаем высоту строки, но не больше содержимого
                    .padding(vertical = 4.dp) // Небольшой внутренний отступ для времени
            ) {
                Text(
                    text = startTimeStr,
                    fontSize = 10.sp,
                    color = contentColor
                )
                // Иконка/Разделитель
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp) // Отступ для иконки
                        .size(10.dp) // Уменьшим размер для компактности
                        .clip(clip) // Используем вашу фигуру
                        .background(iconBackgroundColor)
                )
                Text(
                    text = endTimeStr,
                    fontSize = 10.sp,
                    color = contentColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp)) // Отступ между временем/иконкой и текстом

            // Текст события
            Text(
                text = title,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f) // Занимает оставшееся место
                    .padding(end = 8.dp) // Небольшой отступ справа
            )

            // Сюда можно добавить иконку справа, если нужно
            // Icon(imageVector = Icons.Default.MoreVert, contentDescription = null, tint = contentColor)
        }
    }
}

fun RoundedPolygon.getBounds() = calculateBounds().let { Rect(it[0], it[1], it[2], it[3]) }
class RoundedPolygonShape(
    private val polygon: RoundedPolygon,
    private var matrix: Matrix = Matrix()
) : Shape {
    private var path = Path()
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        path.rewind()
        path = polygon.toPath().asComposePath()
        matrix.reset()
        val bounds = polygon.getBounds()
        val maxDimension = max(bounds.width, bounds.height)
        matrix.scale(size.width / maxDimension, size.height / maxDimension)
        matrix.translate(-bounds.left, -bounds.top)

        path.transform(matrix)
        return Outline.Generic(path)
    }
}

fun createRandomShape(
    isStar: Boolean = Random.nextBoolean(),
    vertices: Int = Random.nextInt(4, 9),
    rounding: CornerRounding = CornerRounding(Random.nextFloat() * 0.7f),
    radius: Float = Random.nextFloat() * 10f
): RoundedPolygon {
    return RoundedPolygon(numVertices = vertices, radius = radius, rounding = rounding)
}



// --- Превью ---
// @Preview(showBackground = true, device = "id:pixel_4", wallpaper = GREEN_DOMINATED_EXAMPLE)
@Composable
fun DefaultPreview() {
    MaterialTheme { // Обертка в тему для стилей Material Design
        CalendarTimelineScreen()
    }
}

// --- Активность (если нужно запустить на устройстве/эмуляторе) ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CalendarTimelineScreen()
            }
        }
    }
}



// фиктивные данные
object SampleData {
    val now: LocalDateTime = LocalDateTime.now()
    val today: LocalDate = now.toLocalDate() // Получаем LocalDate для удобства

    // Генерируем больше событий для разных дат
    val allEvents: List<TimelineEvent> = buildList {
        for (dayOffset in -30..30) { // События за +/- 30 дней от сегодня
            val date = today.plusDays(dayOffset.toLong()) // Работаем с LocalDate
            if (date.dayOfWeek != java.time.DayOfWeek.SATURDAY && date.dayOfWeek != java.time.DayOfWeek.SUNDAY) {
                // Используем date.atTime()
                add(TimelineEvent("docs_${dayOffset}", "Docs sorting", date.atTime(8, 0), date.atTime(9, 30), applicableScale = listOf(TimeScale.Day)))
                add(TimelineEvent("meet_${dayOffset}", "Meet-up", date.atTime(10, 0), date.atTime(11, 30), applicableScale = listOf(TimeScale.Day)))
                add(TimelineEvent("work_${dayOffset}", "Working", date.atTime(11, 0), date.atTime(16, 0), applicableScale = listOf(TimeScale.Day)))
            }
            add(TimelineEvent("swim_${dayOffset}", "Swimming", date.atTime(17, 0), date.atTime(19, 0), applicableScale = listOf(TimeScale.Day, TimeScale.Week)))
            if (date.dayOfWeek == java.time.DayOfWeek.FRIDAY) {
                add(TimelineEvent("dinner_${dayOffset}", "Dinner with friends", date.atTime(20, 0), date.atTime(22, 0), applicableScale = listOf(TimeScale.Day, TimeScale.Week)))
            }
            if (date.dayOfMonth % 5 == 0) {
                add(TimelineEvent("report_${dayOffset}", "Mini Report", date.atTime(9, 0), date.atTime(10, 0), applicableScale = listOf(TimeScale.Day, TimeScale.Week, TimeScale.Month)))
            }
        }
        // Добавим несколько "больших" событий
        // Исправлено: применяем .atTime() к LocalDate
        add(TimelineEvent(
            id ="vac_1",
            title = "Vacation",
            startTime = today.plusMonths(1).withDayOfMonth(5).atTime(0,0), // <- LocalDate.atTime()
            endTime = today.plusMonths(1).withDayOfMonth(15).atTime(23,59), // <- LocalDate.atTime()
            applicableScale = listOf(TimeScale.Week, TimeScale.Month))
        )
        add(TimelineEvent(
            id = "proj_a",
            title = "Project Alpha",
            startTime = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY).atTime(9,0), // <- LocalDate.atTime()
            endTime = today.plusWeeks(1).with(java.time.DayOfWeek.FRIDAY).atTime(17,0), // <- LocalDate.atTime()
            applicableScale = listOf(TimeScale.Week, TimeScale.Month, TimeScale.Year))
        )
        add(TimelineEvent(
            id = "conf_1",
            title = "Big Conference",
            startTime = today.plusMonths(3).withDayOfMonth(10).atTime(8,0), // <- LocalDate.atTime()
            endTime = today.plusMonths(3).withDayOfMonth(12).atTime(18,0), // <- LocalDate.atTime()
            applicableScale = listOf(TimeScale.Month, TimeScale.Year))
        )
    }

    // Функция для получения событий для КОНКРЕТНОГО ПЕРИОДА (без изменений здесь)
    fun getEventsForPeriod(anchorDate: LocalDate, scale: TimeScale): List<TimelineEvent> {
        // ... (логика остается прежней)
        val (periodStart: LocalDateTime, periodEnd: LocalDateTime) = when (scale) {
            TimeScale.Day -> anchorDate.atStartOfDay() to anchorDate.plusDays(1).atStartOfDay()
            TimeScale.Week -> {
                val startOfWeek = anchorDate.with(java.time.DayOfWeek.MONDAY) // или SUNDAY
                startOfWeek.atStartOfDay() to startOfWeek.plusWeeks(1).atStartOfDay()
            }
            TimeScale.Month -> {
                val startOfMonth = anchorDate.withDayOfMonth(1)
                startOfMonth.atStartOfDay() to startOfMonth.plusMonths(1).atStartOfDay()
            }
            TimeScale.Year -> {
                val startOfYear = anchorDate.withDayOfYear(1)
                startOfYear.atStartOfDay() to startOfYear.plusYears(1).atStartOfDay()
            }
        }

        return allEvents.filter { event ->
            event.applicableScale.contains(scale) &&
                    event.startTime.isBefore(periodEnd) &&
                    event.endTime.isAfter(periodStart)
        }.sortedBy { it.startTime }
    }
}