package com.example.caliindar.ui.screens.main.components.calendarui

import RoundedPolygonShape
import androidx.compose.ui.graphics.TileMode
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
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
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

data class GeneratedShapeParams(
    val numVertices: Int,
    val radiusSeed: Float,
    val rotationAngle: Float,
    val shadowOffsetYSeed: Dp,
    val shadowOffsetXSeed: Dp,
    val offestParam: Float,
    // Добавь сюда другие параметры, если нужно
)

@Composable
fun EventListItem(
    event: CalendarEvent,
    timeFormatter: (CalendarEvent) -> String
) {

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
    val cardElevation = if (isCurrentEvent) 8.dp else 0.dp
    val cardActive = if (isCurrentEvent) colorScheme.onSurfaceVariant else colorScheme.primaryContainer
    val cardTextActive = if (isCurrentEvent) colorScheme.surfaceContainer else colorScheme.primaryContainer

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
            .height(65.dp)
    ) {

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd) // То же базовое выравнивание
                .graphicsLayer(
                    // Смещение = базовое смещение + смещение тени
                    translationX = with(density) { (starOffsetX + shapeParams.shadowOffsetXSeed).toPx() },
                    translationY = with(density) { (starOffsetY - shapeParams.shadowOffsetYSeed).toPx() },
                    rotationZ = rotationAngle,
                    // --- Опциональное размытие (требует API 31+) ---
                     renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                         BlurEffect(radiusX = 3f, radiusY = 3f, edgeTreatment = TileMode.Decal)
                     } else null
                )
                .requiredSize(starContainerSize) // Тот же размер
                .clip(clip2Star) // Та же форма
                .background(shadowColor) // Цвет тени
        )
        // --- 4. ОСНОВНАЯ ДЕКОРАТИВНАЯ ФИГУРА (рисуется поверх тени) ---
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd) // Базовое выравнивание
                .graphicsLayer( // Исходное смещение
                    translationX = with(density) { starOffsetX.toPx() },
                    translationY = with(density) { starOffsetY.toPx() },
                    rotationZ = rotationAngle
                )
                .requiredSize(starContainerSize) // Исходный размер
                .clip(clipStar) // Исходная форма
                .background(cardActive.copy(alpha = 0.95f)) // Исходный цвет
        ) // Конец декоративного Box

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 3.dp) // Внутренние отступы для текста
                .heightIn(min = (65 - 8*2).dp) // Минимальная высота для *содержимого* (65dp минус верт. отступы)
            // .wrapContentSize() // Можно использовать, чтобы он не растягивался без нужды
        ) {
            Column(
            ) {
                Text(
                    text = event.summary,
                    color = colorScheme.onPrimaryContainer,
                    style = typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                )

                Text(
                    text = timeFormatter(event),
                    color = colorScheme.onPrimaryContainer,
                    style = typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal
                    ),
                )
            }
        } // Конец внутреннего Box для контента


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