package com.example.caliindar.ui.screens.main.components.calendarui

import android.util.Log
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// --- Константы для UI и логики ---
object CalendarUiDefaults {
    // Размеры и отступы
    val ItemHorizontalPadding = 16.dp
    val ItemVerticalPadding = 3.dp // Базовый вертикальный отступ между элементами
    val MicroItemContentVerticalPadding = 2.dp
    val StandardItemContentVerticalPadding = 8.dp
    val AllDayItemPadding = 16.dp
    val AllDayItemVerticalContentPadding = 6.dp
    val AllDayItemCornerRadius = 25.dp
    val HeaderPadding = 16.dp
    val HeaderVerticalInternalPadding = 8.dp
    val HeaderCornerRadius = 24.dp
    val HeaderBottomSpacing = 8.dp
    val AllDayGroupBottomSpacing = 8.dp
    val LazyColumnBottomPadding = 16.dp
    val EventItemCornerRadius = 20.dp
    val MinEventHeight = 65.dp
    val MaxEventHeight = 200.dp
    val MicroEventHeight = 30.dp
    const val MicroEventMaxDurationMinutes = 30L
    val StarContainerSize = 120.dp // Размер контейнера для декоративной звезды

    // Шрифты
    val HeaderFontSize = 16.sp

    // Тени и Z-index
    val CurrentEventElevation = 8.dp
    val StackedItemZIndexStep = 0.1f
    val StarShadowBlurRadius = 3f

    // Логика группировки
    const val LongEventThresholdMinutes = 120L
    const val SignificantOverlapMinutes = 15L
    const val SimilarDurationPercentThreshold = 0.15 // 15% разницы для "похожей" длительности




}

// --- Вспомогательные функции и классы ---
// (Оставим здесь же для простоты примера, но можно вынести в отдельный файл utils)

// Улучшенный парсер
private val isoOffsetDateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
private val isoLocalDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun parseToInstant(isoString: String?): Instant? {
    if (isoString.isNullOrBlank()) return null
    return try {
        // 1. Пытаемся как OffsetDateTime (наиболее полный формат)
        OffsetDateTime.parse(isoString, isoOffsetDateTimeFormatter).toInstant()
    } catch (e1: DateTimeParseException) {
        try {
            // 2. Пытаемся как LocalDate (если это формат YYYY-MM-DD)
            LocalDate.parse(isoString, isoLocalDateFormatter)
                .atStartOfDay(ZoneOffset.UTC) // Предполагаем UTC для дат без времени
                .toInstant()
        } catch (e2: DateTimeParseException) {
            Log.w("DateTimeParser", "Failed to parse date/time string '$isoString' with known formats.", e2)
            null // Не удалось распознать формат
        }
    } catch (e: Exception) {
        // Общая ошибка, если парсинг не удался по другим причинам
        Log.e("DateTimeParser", "Generic error parsing date/time string: '$isoString'", e)
        null
    }
}
