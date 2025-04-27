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
    val MicroItemContentVerticalPadding = 1.dp
    val StandardItemContentVerticalPadding = 3.dp
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
    const val MicroEventMaxDurationMinutes = 25L
    val MinStarContainerSize = 120.dp // Размер контейнера для декоративной звезды
    val MaxStarContainerSize = 360.dp // Размер контейнера для декоративной звезды

    // Шрифты
    val HeaderFontSize = 16.sp

    // Тени и Z-index
    val CurrentEventElevation = 8.dp
    val StackedItemZIndexStep = 0.1f
    val StarShadowBlurRadius = 3f


    const val HeightSigmoidMidpointMinutes = 180.0
    const val HeightSigmoidScaleFactor = 30.0
    const val HeightSigmoidSteepness = 7.0

    const val ShapeMinVertices = 3
    const val ShapeMaxVerticesDelta = 5 // Vertices = Min + (hash % Delta) -> Range [3, 7] now
    const val ShapeShadowOffsetXMaxModulo = 11 // Range [0, 10]
    const val ShapeShadowOffsetYMin = 3
    const val ShapeShadowOffsetYMaxModulo = 6 // Range [3, 8]
    const val ShapeOffsetParamMultiplier = 0.1f
    const val ShapeRadiusSeedMin = 0.4f
    const val ShapeRadiusSeedRange = 0.4f // Range [0.4f, 0.8f]
    const val ShapeRadiusSeedRangeModulo = 11
    const val ShapeMaxRadius = 3f // Coerce limit
    const val ShapeRotationMaxDegrees = 91 // Range [0, 90]
    const val ShapeRotationOffsetDegrees = -45f // Center range around 0
    const val ShapeCornerRounding = 0.95f
    const val ShapeMainAlpha = 0.95f // Alpha for the main star shape
    const val SHAPEINNERRADIUS = 0.3f // Alpha for the main star shape

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
