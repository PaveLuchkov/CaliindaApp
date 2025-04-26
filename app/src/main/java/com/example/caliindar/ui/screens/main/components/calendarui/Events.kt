package com.example.caliindar.ui.screens.main.components.calendarui

import RoundedPolygonShape
import androidx.compose.ui.graphics.TileMode
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.caliindar.ui.screens.main.CalendarEvent
import com.example.caliindar.ui.screens.main.MainViewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import androidx.compose.runtime.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import com.example.caliindar.ui.theme.LocalFixedAccentColors
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import kotlin.math.exp

data class GeneratedShapeParams(
    val numVertices: Int,
    val radiusSeed: Float,
    val rotationAngle: Float,
    val shadowOffsetYSeed: Dp,
    val shadowOffsetXSeed: Dp,
    val offestParam: Float,
    // Добавь сюда другие параметры, если нужно
)
// --- ДЛЯ ГРУПП
sealed interface EventLayoutGroup {
    val id: String // Уникальный ID для группы (можно использовать ID первого события)
}

data class SingleEventGroup(
    val event: CalendarEvent
) : EventLayoutGroup {
    override val id: String get() = event.id
}

data class StackedEventGroup(
    val events: List<CalendarEvent>, // Отсортированы: самый долгий сверху (или первый по времени начала)
    val isLongEventStack: Boolean // Флаг для различения стеков <120 и >120 мин
) : EventLayoutGroup {
    override val id: String get() = events.firstOrNull()?.id ?: "stacked_${events.hashCode()}"
}

data class OverlayEventGroup(
    val largeEvent: CalendarEvent, // > 120 мин
    val smallEvents: List<CalendarEvent> // < 120 мин, которые накладываются
) : EventLayoutGroup {
    override val id: String get() = largeEvent.id
}








@Composable
fun EventListItem(
    event: CalendarEvent,
    timeFormatter: (CalendarEvent) -> String
) {

    val eventDurationMinutes = remember(event.startTime, event.endTime) {
        val start = parseToInstant(event.startTime)
        val end = parseToInstant(event.endTime)
        if (start != null && end != null && end.isAfter(start)) {
            Duration.between(start, end).toMinutes()
        } else {
            0L // Если время невалидно или конец раньше начала, считаем длительность 0
        }
    }
    val isMicroEvent = remember(eventDurationMinutes) {
        eventDurationMinutes <= 30L && eventDurationMinutes >= 0 // <= 30 минут и не отрицательная
    }

    val targetHeight = remember(isMicroEvent, eventDurationMinutes) {
        val minHeight = 65.dp
        val maxHeight = 200.dp

        if (isMicroEvent) {
            30.dp // Фиксированная высота для микро-событий
        } else {
            // Динамическая высота для НЕ-микро событий
            val durationDouble = eventDurationMinutes.toDouble()
            val heightRange = maxHeight - minHeight // Диапазон для масштабирования (135.dp)

            // Масштабированный ввод для сигмоиды (экспериментальные параметры)
            // Центрируем вокруг 90 минут, диапазон ~30-180 минут отображаем на ~[-2, 3]
            val x = (durationDouble - 120.0) / 30.0
            val k = 1.0 // Коэффициент крутизны (можно подбирать)
            val sigmoidOutput = 1.0 / (1.0 + exp(-k * x))

            // Масштабируем выход сигмоиды (0..1) к диапазону высот [minHeight, maxHeight]
            val calculatedHeight = minHeight + (heightRange * sigmoidOutput.toFloat())

            // Зажимаем результат в пределах min/max на всякий случай
            calculatedHeight.coerceIn(minHeight, maxHeight)
        }
    }



    val shapeParams = remember(event.id) { // Используем event.id как стабильный ключ
        val hashCode = event.summary.hashCode()

        // Модуль 7 дает 0-6. Добавляем 3, получаем диапазон [3, 9].
        val numVertices = (abs(hashCode) % 5) + 3
        val shadowOffsetXSeed = ((abs(hashCode) % 11))
        val shadowOffsetYSeed = (abs(hashCode) % 6 + 3)
        val offestParam = ((abs(hashCode) % 4 + 1) * 0.1f)

        // 2. Соотношение внутреннего радиуса (например, от 0.4f до 0.8f)
        // Используем другую операцию с хэшем, чтобы немного отвязать от кол-ва вершин
        // Модуль 11 дает 0-10. Умножаем на 0.04f -> 0.0f - 0.4f. Добавляем 0.4f -> [0.4f, 0.8f]
        val ratioSeed = abs(hashCode / 3 + 42) // Просто другая арифметика над хэшем
        val radiusSeed = ((ratioSeed % 11) * 0.4f + 0.4f)
            .coerceIn(0.4f, 3f) // Ограничиваем на всякий случай

        val angleSeed = abs(hashCode / 5 - 99) % 91 // 0-90
        val rotationAngle = (angleSeed - 45).toFloat()

        GeneratedShapeParams(
            numVertices = numVertices,
            radiusSeed = radiusSeed,
            rotationAngle = rotationAngle,
            shadowOffsetXSeed = shadowOffsetXSeed.dp,
            shadowOffsetYSeed = shadowOffsetYSeed.dp,
            offestParam = offestParam
        )
    }

    val starShape = remember(shapeParams.numVertices, shapeParams.radiusSeed) { // Пересоздаем полигон, если параметры изменились
        RoundedPolygon.star(
            numVerticesPerRadius = shapeParams.numVertices,
            radius = shapeParams.radiusSeed, // Оставляем фиксированным, размер контролируем ниже
            rounding = CornerRounding(0.95f) // Используем сглаживание углов
        )
    }
    val clipStar = remember(starShape) { // Пересоздаем Shape, если полигон изменился
        RoundedPolygonShape(polygon = starShape)
    }
    val clip2Star = remember(starShape) { // Пересоздаем Shape, если полигон изменился
        RoundedPolygonShape(polygon = starShape)
    }

    val starContainerSize = 120.dp
    val starOffsetY = starContainerSize * shapeParams.offestParam
    val starOffsetX = starContainerSize * -shapeParams.offestParam
    val rotationAngle = remember(event.id) {
        (event.summary.hashCode() % 45).toFloat()
    }

    // Параметры тени
    val shadowColor = Color.Black.copy(alpha = 0.3f)
    val density = LocalDensity.current
    val darkerShadowColor = Color.Black


    // --- ПАРАМЕТРЫ СОБЫТИЙ
    var isCurrentEvent by remember(event.id) { mutableStateOf(false) }

    LaunchedEffect(key1 = event.id) { // Перезапускаем эффект, если изменился ID события
        while (isActive) { // Цикл работает, пока корутина активна
            val now = Instant.now()
            val startTimeInstant = parseToInstant(event.startTime)
            val endTimeInstant = parseToInstant(event.endTime)

            // Проверяем, находится ли 'now' в диапазоне [start, end)
            isCurrentEvent = if (startTimeInstant != null && endTimeInstant != null) {
                !now.isBefore(startTimeInstant) && now.isBefore(endTimeInstant)
            } else {
                false // Если время невалидно, считаем неактивным
            }

            // Log.d("EventListItem", "Event '${event.summary}' isCurrent: $isCurrentEvent (Now: $now, Start: $startTimeInstant, End: $endTimeInstant)") // Для отладки

            delay(60000L) // Пауза на 1 минуту (60 * 1000 мс)
        }
    }
    val fixedColors = LocalFixedAccentColors.current

    val cardElevation = if (isCurrentEvent) 8.dp else 0.dp
    val cardActive = if (isCurrentEvent) fixedColors.primaryFixed  else colorScheme.primaryContainer
    val cardTextActive = if (isCurrentEvent) fixedColors.onPrimaryFixed else colorScheme.onPrimaryContainer

    // 1. Внешний Box: отвечает за фон, форму карточки и служит контейнером для выравнивания
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp) // Отступы вокруг карточки
            .shadow(
                elevation = cardElevation, // Условная элевация
                shape = RoundedCornerShape(20.dp), // Форма тени совпадает с формой карточки
                clip = false, // ВАЖНО: false, чтобы тень рисовалась снаружи clip'а
                ambientColor = if (cardElevation > 0.dp) darkerShadowColor else Color.Transparent,
                spotColor = if (cardElevation > 0.dp) darkerShadowColor else Color.Transparent
            )
            .clip(RoundedCornerShape(20.dp))
            .background(cardActive)
            .height(targetHeight)
    ) {

        if (!isMicroEvent) { // Показываем декор только для НЕ-микро
            // Тень
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .graphicsLayer(
                        translationX = with(density) { (starOffsetX + shapeParams.shadowOffsetXSeed).toPx() },
                        translationY = with(density) { (starOffsetY - shapeParams.shadowOffsetYSeed).toPx() },
                        rotationZ = rotationAngle,
                        renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            BlurEffect(radiusX = 3f, radiusY = 3f, edgeTreatment = TileMode.Decal)
                        } else null
                    )
                    .requiredSize(starContainerSize)
                    .clip(clip2Star)
                    .background(shadowColor)
            )
            // Основная фигура
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .graphicsLayer(
                        translationX = with(density) { starOffsetX.toPx() },
                        translationY = with(density) { starOffsetY.toPx() },
                        rotationZ = rotationAngle
                    )
                    .requiredSize(starContainerSize)
                    .clip(clipStar)
                    .background(cardActive.copy(alpha = 0.95f)) // Можно использовать cardBaseColor, если введен
            )
        } // Конец условия if (!isMicroEvent)

        // --- ТЕКСТОВЫЙ КОНТЕНТ ---
        Box(
            modifier = Modifier
                .fillMaxSize() // Занимаем всю доступную высоту карточки
                .padding(horizontal = 16.dp, vertical = if(isMicroEvent) 2.dp else 8.dp) // Разные верт. отступы
            ,
            contentAlignment = Alignment.TopStart // Выравниваем содержимое внутри этого Box
        ) {
            if (isMicroEvent) {
                // --- МИКРО-СОБЫТИЕ: Row ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically // Выравниваем текст по центру строки
                ) {
                    Text(
                        text = event.summary,
                        color = cardTextActive,
                        // Используем более компактный стиль для микро-событий
                        style = typography.titleLarge.copy(fontWeight = FontWeight.Medium), // или bodyMedium
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // Даем тексту заголовка занять место, но не выталкивать время
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // Отступ между текстами
                    Text(
                        text = timeFormatter(event), // Время события
                        color = cardTextActive,
                        // Тот же или похожий компактный стиль
                        style = typography.labelMedium, // или bodySmall
                        maxLines = 1
                    )
                }
            } else {
                // --- НЕ-МИКРО-СОБЫТИЕ: Column (как было) ---
                Column(
                    verticalArrangement = Arrangement.Center // Центрируем колонку по вертикали
                ) {
                    Text(
                        text = event.summary,
                        color = cardTextActive,
                        style = typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2, // Оставляем 2 строки для длинных названий
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timeFormatter(event),
                        color = cardTextActive,
                        style = typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                        maxLines = 1
                    )
                }
            } // Конец внутреннего Box для контента
        }

    } // Конец внешнего Box
}

private fun parseToInstant(isoString: String?): Instant? {
    if (isoString.isNullOrBlank()) return null
    return try {
        // Пытаемся как OffsetDateTime (со смещением или Z)
        OffsetDateTime.parse(isoString).toInstant()
    } catch (e: DateTimeParseException) {
        try {
            // Пытаемся как LocalDate (для событий 'all-day', YYYY-MM-DD)
            // Считаем, что такое событие начинается в 00:00 UTC этого дня
            // Важно: Это может быть не совсем точное представление all-day событий
            // в контексте "текущего" момента, но для примера подойдет.
            java.time.LocalDate.parse(isoString)
                .atStartOfDay(ZoneOffset.UTC) // Начало дня в UTC
                .toInstant()
        } catch (e2: DateTimeParseException) {
            Log.w("EventListItem", "Failed to parse date/time string: $isoString", e2)
            null // Не удалось распознать формат
        }
    } catch (e: Exception) {
        Log.w("EventListItem", "Generic error parsing date/time string: $isoString", e)
        null
    }
}

@Composable
fun EventsList(
    events: List<CalendarEvent>,
    timeFormatter: (CalendarEvent) -> String,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        //contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items = events, key = { it.id }) { event ->
            EventListItem(
                event = event,
                timeFormatter = timeFormatter
            )
        }
    }
}


@Composable
fun DayEventsPage(
    date: LocalDate,
    viewModel: MainViewModel,
) {
    val eventsFlow = remember(date) { viewModel.getEventsFlowForDate(date) }
    val eventsState = eventsFlow.collectAsStateWithLifecycle(initialValue = emptyList()) //Returns State<List<CalendarEvent>>
    val events = eventsState.value

    val (allDayEvents, timedEvents) = remember(events) { // Запоминаем результат разделения
        events.partition { it.isAllDay }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.
        fillMaxSize()
            .padding(vertical = 16.dp)) {
            // Заголовок Дня (можно вынести в отдельный Composable)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(color = colorScheme.primary)
            ){
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))),
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth(),// Больше отступы
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                )
            }
            if (allDayEvents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp)) // Отступ после заголовка даты
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp) // Общий горизонтальный отступ
                ) {
                    allDayEvents.forEach { event ->
                        AllDayEventItem(event = event) // Используем новый Composable
                        Spacer(modifier = Modifier.height(3.dp)) // Отступ между элементами "весь день"
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Список Событий для этого дня
            if (timedEvents.isNotEmpty()) {
                EventsList(
                    events = timedEvents, // Передаем только события со временем
                    timeFormatter = viewModel::formatEventListTime,
                    modifier = Modifier
                        .weight(1f) // Занимает оставшееся место
                        .fillMaxWidth()
                )
            } else if (allDayEvents.isEmpty()) {
                // Показываем сообщение "нет событий", только если НЕТ НИКАКИХ событий
                Box(
                    modifier = Modifier
                        .weight(1f) // Занимает место списка
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center // Центрируем сообщение
                ) {
                    Text("На эту дату событий нет", style = typography.bodyLarge)
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        } // End Column
    } // End Box
}


@Composable
fun AllDayEventItem(event: CalendarEvent) {
    val colorScheme = colorScheme
    val typography = typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(25.dp))
            .background(colorScheme.tertiary)
            .padding(horizontal = 16.dp, vertical = 6.dp) // Вертикальный отступ чуть больше
    ) {
        Text(
            text = event.summary,
            style = typography.bodyLarge, // Стиль можно подобрать
            fontWeight = FontWeight.Medium,
            color = colorScheme.onTertiary,
            textAlign = TextAlign.Center, // Или TextAlign.Start
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
        )
    }
}





