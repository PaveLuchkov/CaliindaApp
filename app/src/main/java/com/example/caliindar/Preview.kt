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
import androidx.compose.animation.core.tween // Для настройки скорости анимации
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.lazy.LazyListState // Убедитесь, что импорт есть
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion
import kotlinx.coroutines.launch // Для запуска корутины сброса скролла
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture // <<< Импорт
import androidx.compose.ui.input.pointer.* // <<< Импорт для PointerEvent, PointerId и т.д.
import kotlin.math.abs // Для abs()
import kotlin.math.sqrt

//  уровни масштаба
enum class TimeScale {
    Day, Week, Month, Year
}



data class TimelineEvent(
    val id: String, // Уникальный идентификатор
    val title: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val description: String? = null,
    // Можно добавить цвет, иконку и т.д.
    val applicableScale: List<TimeScale> // Указываем, на каких масштабах событие релевантно
)

@Immutable
data class TimelineKey(val date: LocalDate, val scale: TimeScale)

fun getNextScale(current: TimeScale): TimeScale? {
    return when (current) {
        TimeScale.Year -> TimeScale.Month
        TimeScale.Month -> TimeScale.Week
        TimeScale.Week -> TimeScale.Day
        TimeScale.Day -> null // Дальше некуда
    }
}

fun getPreviousScale(current: TimeScale): TimeScale? {
    return when (current) {
        TimeScale.Day -> TimeScale.Week
        TimeScale.Week -> TimeScale.Month
        TimeScale.Month -> TimeScale.Year
        TimeScale.Year -> null // Дальше некуда
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Preview(showBackground = true, device = "id:pixel_6a")
@Composable
fun CalendarTimelineScreen() {

    // --- Состояния ---
    var currentScale by remember { mutableStateOf(TimeScale.Day) }
        //  var currentZoom by remember { mutableStateOf(1f) }
    // Добавляем состояние для текущей даты/периода
    var currentDateAnchor by remember { mutableStateOf(LocalDate.now()) }
    val listState = rememberLazyListState() // Состояние для LazyColumn

    val timelineKey by remember {
        derivedStateOf { TimelineKey(currentDateAnchor, currentScale) }
    }

    // --- Константы и Помощники ---
    val zoomSensitivityFactor = 1.5f // <<< Увеличьте/уменьшите для настройки чувствительности зума
    val zoomThresholds = remember {
        mapOf(
            // Порог для перехода ОТ ЭТОГО МАСШТАБА к ПРЕДЫДУЩЕМУ (более мелкому)
            TimeScale.Day to 0.7f,
            TimeScale.Week to 0.4f,
            TimeScale.Month to 0.2f,
            TimeScale.Year to 0.0f, // Не используется для zoom-out
        )
    }
    // Пороги для Zoom IN (переход к более детальному масштабу)
    val zoomInThresholds = remember {
        mapOf(
            TimeScale.Year to 2.0f,  // От Года к Месяцу
            TimeScale.Month to 1.8f, // От Месяца к Неделе
            TimeScale.Week to 1.6f,  // От Недели к Дню
            TimeScale.Day to Float.MAX_VALUE // От Дня некуда
        )
    }
    val currentDateFormat = remember(currentScale) { // Формат для отображения текущего периода
        when (currentScale) {
            TimeScale.Day -> DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
            TimeScale.Week -> DateTimeFormatter.ofPattern("'Week of' d MMM yyyy") // Пример
            TimeScale.Month -> DateTimeFormatter.ofPattern("MMMM yyyy")
            TimeScale.Year -> DateTimeFormatter.ofPattern("yyyy")
        }
    }

    // --- Логика получения данных (зависит от scale и date) ---
    // Используем derivedStateOf для оптимизации: пересчет только при изменении зависимостей
    val eventsToShow by remember(currentScale, currentDateAnchor) {
        derivedStateOf {
            Log.d("Timeline", "Fetching events for $currentScale starting $currentDateAnchor") // Для отладки
            SampleData.getEventsForPeriod(currentDateAnchor, currentScale)
        }
    }


    // --- Функции навигации по времени ---
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

    // --- Nested Scroll Connection для перехвата overscroll ---
    val nestedScrollConnection = remember {
        // Состояние для накопления overscroll ВВЕРХ (когда тянем список вниз на верхнем краю)
        var accumulatedOverscrollUp by mutableStateOf(0f)
        // Состояние для накопления overscroll ВНИЗ (когда тянем список вверх на нижнем краю)
        var accumulatedOverscrollDown by mutableStateOf(0f)
        // Порог, который нужно превысить для навигации (подбирается экспериментально)
        val overscrollNavigationThreshold = 200f // <<< Попробуйте это значение, можно увеличить/уменьшить

        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Работаем только с жестом перетаскивания
                if (source == NestedScrollSource.UserInput) {
                    val deltaY = available.y
                    val canScrollBackward = listState.canScrollBackward
                    val canScrollForward = listState.canScrollForward

                    when {
                        // Пытаемся скроллить ВВЕРХ (тянем палец ВНИЗ, deltaY > 0), но УЖЕ наверху (!canScrollBackward)
                        deltaY > 0 && !canScrollBackward -> {
                            accumulatedOverscrollUp += deltaY
                            // Сбрасываем накопление другого направления, если вдруг было
                            accumulatedOverscrollDown = 0f
                            Log.d("TimelineScroll", "Overscroll UP accumulation: $accumulatedOverscrollUp / $overscrollNavigationThreshold")

                            // Проверяем порог
                            if (accumulatedOverscrollUp >= overscrollNavigationThreshold) {
                                Log.d("TimelineScroll", "--> Navigating Previous Period")
                                navigateToPreviousPeriod()
                                accumulatedOverscrollUp = 0f // Сброс после навигации
                            }
                            // "Съедаем" скролл, чтобы список не двигался
                            return Offset(0f, available.y) // Поглощаем вертикальный скролл
                        }

                        // Пытаемся скроллить ВНИЗ (тянем палец ВВЕРХ, deltaY < 0), но УЖЕ внизу (!canScrollForward)
                        deltaY < 0 && !canScrollForward -> {
                            // Накапливаем абсолютное значение
                            accumulatedOverscrollDown += abs(deltaY)
                            // Сбрасываем накопление другого направления
                            accumulatedOverscrollUp = 0f
                            Log.d("TimelineScroll", "Overscroll DOWN accumulation: $accumulatedOverscrollDown / $overscrollNavigationThreshold")

                            // Проверяем порог
                            if (accumulatedOverscrollDown >= overscrollNavigationThreshold) {
                                Log.d("TimelineScroll", "--> Navigating Next Period")
                                navigateToNextPeriod()
                                accumulatedOverscrollDown = 0f // Сброс после навигации
                            }
                            // "Съедаем" скролл
                            return Offset(0f, available.y) // Поглощаем вертикальный скролл
                        }

                        // Во всех остальных случаях (скролл в середине списка, скролл ОТ края)
                        else -> {
                            // Сбрасываем аккумуляторы, так как мы больше не в состоянии overscroll
                            // или начали скроллить в другую сторону
                            accumulatedOverscrollUp = 0f
                            accumulatedOverscrollDown = 0f
                            // Позволяем LazyColumn обработать скролл как обычно
                            return Offset.Zero
                        }
                    }
                }
                return Offset.Zero // Не обрабатываем другие источники скролла (fling и т.д.)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Если после обработки скролла LazyColumn'ом что-то осталось (например, при fling),
                // можно тоже сбросить аккумуляторы здесь, на всякий случай.
                // Хотя логика в onPreScroll должна покрывать большинство случаев сброса.
                if(source == NestedScrollSource.UserInput && available.y != 0f) {
                    // Если есть остаточный скролл после обработки LazyColumn'ом,
                    // возможно, мы достигли края в этом же событии, сбросим на всякий.
                    // accumulatedOverscrollUp = 0f
                    // accumulatedOverscrollDown = 0f
                    // Но обычно это не требуется, так как onPreScroll сработает первым при следующем событии.
                }
                // Важно сбрасывать при окончании жеста, но NestedScrollConnection не дает прямого колбека
                // на окончание *всего* жеста. Сброс в `else` ветке onPreScroll обычно достаточен.
                return super.onPostScroll(consumed, available, source)
            }



            // Можно также реализовать onPostScroll или onPreFling/onPostFling при необходимости
        }
    }

    Scaffold(
        // Обновляем AppBar, чтобы показывать текущую дату/период
        topBar = { CalendarAppBar(title = currentDateAnchor.format(currentDateFormat)) },
        bottomBar = { CalendarBottomBar() },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->

        var gestureZoomLevel by rememberSaveable { mutableStateOf(1f) }

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                // --- Начало pointerInput с ручной обработкой ---
                .pointerInput(Unit) {
                    // awaitEachGesture гарантирует сброс состояния для каждого нового жеста
                    awaitEachGesture {
                        // --- Состояние для ЭТОГО КОНКРЕТНОГО ЖЕСТА ---
                        var gestureZoomLevel = 1.0f // Накопленный зум для текущего жеста
                        var scaleChangedDuringThisGesture = false // Блокировка смены масштаба
                        var pastTouchSlop = false // Пройден ли порог случайного касания
                        var lockedPointerId1: PointerId? = null
                        var lockedPointerId2: PointerId? = null
                        var lastDistance = -1.0f // Предыдущее расстояние между пальцами

                        Log.d("ZoomGesture", "--- New Gesture Sequence ---")

                        // Цикл обработки событий внутри текущего жеста
                        while (true) {
                            val event: PointerEvent = awaitPointerEvent(PointerEventPass.Main)
                            val changes = event.changes

                            // --- Проверка окончания жеста ---
                            if (changes.all { !it.pressed }) {
                                Log.d("ZoomGesture", "Gesture End Detected (all pointers up)")
                                break // Выход из цикла while для этого жеста
                            }

                            // --- Отслеживание двух пальцев для зума ---
                            val pointers = changes.filter { it.pressed }

                            // Если количество пальцев изменилось (стало не 2), сбрасываем трекинг зума
                            if (pointers.size != 2 && lockedPointerId1 != null) {
                                Log.d("ZoomGesture", "Pointer count != 2, resetting internal zoom track.")
                                gestureZoomLevel = 1.0f // Сброс зума в рамках жеста
                                lastDistance = -1.0f
                                lockedPointerId1 = null
                                lockedPointerId2 = null
                                pastTouchSlop = false
                                continue // Переход к следующему событию
                            }

                            // --- Обработка, если ровно два пальца нажаты ---
                            if (pointers.size == 2) {
                                // Если еще не зафиксировали пальцы, делаем это
                                if (lockedPointerId1 == null) {
                                    lockedPointerId1 = pointers[0].id
                                    lockedPointerId2 = pointers[1].id
                                    lastDistance = -1f // Сброс для первого расчета
                                    Log.d("ZoomGesture", "Locked on pointers: $lockedPointerId1, $lockedPointerId2")
                                }

                                // Находим изменения для зафиксированных пальцев
                                val pointer1Change = changes.find { it.id == lockedPointerId1 }
                                val pointer2Change = changes.find { it.id == lockedPointerId2 }

                                if (pointer1Change != null && pointer2Change != null) {
                                    // --- Проверка Touch Slop ---
                                    if (!pastTouchSlop) {
                                        val slop = viewConfiguration.touchSlop
                                        // Простое смещение хотя бы одного пальца
                                        val distance1 = pointer1Change.position - pointer1Change.previousPosition
                                        val distance2 = pointer2Change.position - pointer2Change.previousPosition
                                        if (distance1.getDistanceSquared() > slop * slop || distance2.getDistanceSquared() > slop * slop) {
                                            pastTouchSlop = true
                                            Log.d("ZoomGesture", "Touch slop passed")
                                            // Важно: пересчитать lastDistance после прохождения slop
                                            val dxInit = pointer1Change.position.x - pointer2Change.position.x
                                            val dyInit = pointer1Change.position.y - pointer2Change.position.y
                                            lastDistance = sqrt(dxInit*dxInit + dyInit*dyInit)

                                        }
                                    }

                                    // --- Вычисление и применение зума (только после touch slop) ---
                                    if (pastTouchSlop) {
                                        val dx = pointer1Change.position.x - pointer2Change.position.x
                                        val dy = pointer1Change.position.y - pointer2Change.position.y
                                        val currentDistance = sqrt(dx*dx + dy*dy)

                                        // Вычисляем множитель зума для текущего шага
                                        if (lastDistance > 0.01f) { // Избегаем деления на ноль/малое число
                                            val stepZoom = currentDistance / lastDistance
                                            val effectiveStepZoom = 1.0f + (stepZoom - 1.0f) * zoomSensitivityFactor

                                            // Накапливаем зум для всего жеста
                                            gestureZoomLevel *= effectiveStepZoom
                                            gestureZoomLevel = gestureZoomLevel.coerceIn(0.05f, 5.0f) // Ограничиваем

                                            Log.d("ZoomGesture", "Step: Dist=$currentDistance, StepZoom=$stepZoom, AccumZoom=$gestureZoomLevel, Locked=$scaleChangedDuringThisGesture")


                                            // --- Логика смены масштаба (та же, что была) ---
                                            if (!scaleChangedDuringThisGesture) {
                                                val zoomOutThreshold = zoomThresholds[currentScale] ?: 0f
                                                val previousScale = getPreviousScale(currentScale)
                                                val zoomInThreshold = zoomInThresholds[currentScale] ?: Float.MAX_VALUE
                                                val nextScale = getNextScale(currentScale)
                                                var scaleChanged = false

                                                // Проверяем Zoom Out
                                                if (previousScale != null && gestureZoomLevel < zoomOutThreshold) {
                                                    Log.d("ZoomGesture", "--> Zoom Out Triggered: $currentScale -> $previousScale")
                                                    currentScale = previousScale
                                                    scaleChanged = true
                                                }
                                                // Проверяем Zoom In
                                                else if (nextScale != null && gestureZoomLevel > zoomInThreshold) {
                                                    Log.d("ZoomGesture", "--> Zoom In Triggered: $currentScale -> $nextScale")
                                                    currentScale = nextScale
                                                    scaleChanged = true
                                                }

                                                // Если масштаб изменился
                                                if (scaleChanged) {
                                                    Log.d("ZoomGesture", "** Scale Changed to: $currentScale! Locking gesture. **")
                                                    scaleChangedDuringThisGesture = true // Блокируем
                                                    currentDateAnchor = when(currentScale) {
                                                        TimeScale.Day -> currentDateAnchor // Оставляем тот же день
                                                        TimeScale.Week -> currentDateAnchor.with(java.time.DayOfWeek.MONDAY) // Начало недели
                                                        TimeScale.Month -> currentDateAnchor.withDayOfMonth(1) // Начало месяца
                                                        TimeScale.Year -> currentDateAnchor.withDayOfYear(1) // Начало года
                                                    }
                                                    // Сброс зума для начала накопления в новом масштабе
                                                    gestureZoomLevel = 1.0f
                                                    // Сброс расстояния, т.к. относительный зум изменился
                                                    lastDistance = -1.0f
                                                }
                                            } // Конец if (!scaleChangedDuringThisGesture)
                                        } // Конец if (lastDistance > 0.01f)

                                        // Обновляем предыдущее расстояние для следующего шага, если не сбросили
                                        if(lastDistance > 0) {
                                            lastDistance = currentDistance
                                        }

                                    } // Конец if (pastTouchSlop)
                                } else {
                                    // Один из пальцев "потерялся" - сброс трекинга
                                    Log.d("ZoomGesture", "Lost locked pointer, resetting internal zoom track.")
                                    gestureZoomLevel = 1.0f
                                    lastDistance = -1.0f
                                    lockedPointerId1 = null
                                    lockedPointerId2 = null
                                    pastTouchSlop = false
                                }
                            } // Конец if (pointers.size == 2)

                            // Потребляем события, чтобы они не шли дальше (если нужно)
                            changes.forEach { it.consume() }

                        } // Конец цикла while(true)
                    } // Конец awaitEachGesture
                } // Конец pointerInput
            // --- Конец pointerInput с ручной обработкой ---
        ) {
            BackgroundShapes(MaterialTheme.colorScheme)

            // --- Анимация смены контента ---
            AnimatedContent(
                targetState = timelineKey, // Реагируем на изменение ключа (даты или масштаба)
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    // Определяем направление: вперед или назад по дате?
                    val goingBackwards = targetState.date.isBefore(initialState.date)
                    // Можно добавить логику для масштаба, если нужна другая анимация

                    val enterTransition: EnterTransition
                    val exitTransition: ExitTransition
                    val animationDuration = 200 // Длительность анимации

                    if (goingBackwards) {
                        // Переход НАЗАД: старый уходит ВНИЗ, новый приходит СВЕРХУ
                        enterTransition = slideInVertically(animationSpec = tween(animationDuration)) { height -> -height } + fadeIn(tween(animationDuration))
                        exitTransition = slideOutVertically(animationSpec = tween(animationDuration)) { height -> height } + fadeOut(tween(animationDuration))
                    } else {
                        // Переход ВПЕРЕД: старый уходит ВВЕРХ, новый приходит СНИЗУ
                        enterTransition = slideInVertically(animationSpec = tween(animationDuration)) { height -> height } + fadeIn(tween(animationDuration))
                        exitTransition = slideOutVertically(animationSpec = tween(animationDuration)) { height -> -height } + fadeOut(tween(animationDuration))
                    }


//                     if (targetState.scale != initialState.scale) {
//                         enterTransition = fadeIn(...) + scaleIn(...)
//                         exitTransition = fadeOut(...) + scaleOut(...)
//                     }

                    enterTransition togetherWith exitTransition using(
                            SizeTransform(clip = false) // Позволяет выезжать за границы
                            )
                }
            ) { key -> // Лямбда контента получает текущий ключ (key = timelineKey)

                // >>> ВАЖНО: Сбрасываем скролл в начало при смене ключа <<<
                // Используем LaunchedEffect, который сработает, когда 'key' изменится
                LaunchedEffect(key) {
                    // Проверяем, что список не пуст, прежде чем скроллить
                    if (listState.layoutInfo.totalItemsCount > 0) {
                        Log.d("TimelineAnimate", "Scrolling to top for key: $key")
                        listState.scrollToItem(0) // Скролл к первому элементу
                    }
                }


                TimelineContent(
                    events = eventsToShow, // Используем .value от derivedStateOf
                    currentScale = key.scale,   // Можно использовать key.scale
                    listState = listState,      // <<< Передаем тот же самый listState
                    modifier = Modifier.fillMaxSize()
            )
                }
        }
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
        state = listState, // <<< Используем переданное состояние
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp) // Отступы внутри списка
    ) {
        if (events.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No events for this period", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(events, key = { it.id }) { event ->
                TimelineEventRowAdapter(event = event, currentScale = currentScale)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun TimelineEventRowAdapter(event: TimelineEvent, currentScale: TimeScale) {
    // Пока используем один и тот же EventCard, но передаем больше информации
    // В будущем здесь можно будет использовать РАЗНЫЕ @Composable функции
    // в зависимости от currentScale
    when (currentScale) {
        TimeScale.Day -> DayTimelineEventRow(event = event)
        TimeScale.Week, TimeScale.Month, TimeScale.Year -> GenericTimelineEventRow(event = event, scale = currentScale)
        // Добавьте другие варианты отображения по необходимости
    }
}

// --- Компонент для строки события Дня  ---
val timeFormatter = DateTimeFormatter.ofPattern("H:mm")
val dateFormatter = DateTimeFormatter.ofPattern("dd MMM")
val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM H:mm")

@Composable
fun DayTimelineEventRow(event: TimelineEvent) {
    val isCurrent = remember(event) { // Простая проверка на "текущее" (пересекает текущий момент)
        val now = LocalDateTime.now()
        event.startTime.isBefore(now) && event.endTime.isAfter(now)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Уменьшим вертикальный отступ
        verticalAlignment = Alignment.Top // Выравниваем по верху
    ) {
        // Можно вернуть Box с линией таймлайна слева, если нужно

        Spacer(modifier = Modifier.width(16.dp)) // Отступ для линии таймлайна (если будет)

        // Используем ваш EventCard или адаптированную версию
        EventCard(
            title = event.title,
            isCurrent = isCurrent,
            startTimeStr = event.startTime.format(timeFormatter), // Форматируем время
            endTimeStr = event.endTime.format(timeFormatter)
        )
    }
}

@Composable
fun GenericTimelineEventRow(event: TimelineEvent, scale: TimeScale) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp) // Добавим горизонтальный отступ
            .height(IntrinsicSize.Min), // Высота по содержимому
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f).padding(end = 8.dp) // Занимать доступное место
            )
            // Показываем даты начала/конца
            Text(
                text = "${event.startTime.format(dateFormatter)} - ${event.endTime.format(dateFormatter)}",
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
       // title = { Text("Caliinda", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        navigationIcon = {
            IconButton(onClick = {}) {
                Icon(Icons.Filled.Today, contentDescription = "Calendar View")
            }
        },
        actions = {
            IconButton(onClick = { /* Действие для настроек */ }) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        },
    )
}

// --- Нижняя панель навигации/действий ---
@Composable
fun CalendarBottomBar() {
    BottomAppBar(
        backgroundColor = colorScheme.surface,
        cutoutShape = CircleShape // Можно добавить вырез для FAB если нужно
    ) {
        IconButton(onClick = { /* TODO: Действие клавиатуры */ }) {
            Icon(Icons.Filled.Keyboard, contentDescription = "Keyboard Input")
        }
        Spacer(Modifier.weight(1f)) // Занимает все доступное пространство
        FloatingActionButton(
            onClick = {},
            modifier = Modifier
                .size(56.dp))
        {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "Voice Input")
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
        //        radius = 30f,
            )
        }
        val clipStar = remember(starShape) {
            RoundedPolygonShape(polygon = starShape)
        }
        val star2Shape = remember {
            RoundedPolygon.star(
                4,
                rounding = CornerRounding(0.6f),
                //        radius = 30f,
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
    // NOTE: Использование createRandomShape() в remember может привести к тому,
    // что форма будет меняться при рекомпозиции в неожиданных местах.
    // Лучше генерировать форму вне Composable или передавать ее как параметр.
    // Пока оставим для примера.
    val polygon = remember { createRandomShape() }
    val clip = remember(polygon) { RoundedPolygonShape(polygon = polygon) }

    // Используем цвета из MaterialTheme.colorScheme
    val backgroundColor = if (isCurrent) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (isCurrent) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
    val iconBackgroundColor = contentColor // Фон иконки делаем таким же, как цвет контента

    Card(
        modifier = Modifier
            // Высота должна зависеть от контента или быть фиксированной, но аккуратно
            .height(IntrinsicSize.Min) // Попробуем высоту по контенту
            // .height(if (isCurrent) 100.dp else 65.dp) // Ваши значения, но осторожно с обрезанием текста
            .fillMaxWidth(),
        //.padding(end = 80.dp), // Этот padding лучше убрать, пусть Row внутри управляет отступами
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