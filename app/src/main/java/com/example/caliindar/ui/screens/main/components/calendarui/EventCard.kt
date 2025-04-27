package com.example.caliindar.ui.screens.main.components.calendarui

import RoundedPolygonShape
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import com.example.caliindar.ui.screens.main.CalendarEvent
import com.example.caliindar.ui.theme.LocalFixedAccentColors
import java.time.Duration
import kotlin.math.abs
import kotlin.math.exp

@Composable
fun EventListItem(
    event: CalendarEvent,
    timeFormatter: (CalendarEvent) -> String,
    isCurrentEvent: Boolean, // Получаем снаружи
    modifier: Modifier = Modifier // Позволяем контейнеру управлять внешними отступами/размером
) {
    // --- Расчет высоты  ---
    val eventDurationMinutes = remember(event.startTime, event.endTime) {
        val start = parseToInstant(event.startTime)
        val end = parseToInstant(event.endTime)
        if (start != null && end != null && end.isAfter(start)) {
            Duration.between(start, end).toMinutes()
        } else {
            0L
        }
    }
    val isMicroEvent = remember(eventDurationMinutes) {
        eventDurationMinutes <= CalendarUiDefaults.MicroEventMaxDurationMinutes && eventDurationMinutes >= 0
    }
    val targetHeight = remember(isMicroEvent, eventDurationMinutes) {
        val minHeight = CalendarUiDefaults.MinEventHeight
        val maxHeight = CalendarUiDefaults.MaxEventHeight

        if (isMicroEvent) {
            CalendarUiDefaults.MicroEventHeight
        } else {
            val durationDouble = eventDurationMinutes.toDouble()
            val heightRange = maxHeight - minHeight
            val x = (durationDouble - 120.0) / 30.0 // Параметры сигмоиды можно вынести в константы, если нужно
            val k = 1.0
            val sigmoidOutput = 1.0 / (1.0 + exp(-k * x))
            val calculatedHeight = minHeight + (heightRange * sigmoidOutput.toFloat())
            calculatedHeight.coerceIn(minHeight, maxHeight)
        }
    }

    // --- Генерация формы (используем id, константы) (оставлена без изменений по запросу) ---
    val shapeParams = remember(event.id) { // Используем event.id как стабильный ключ
        val hashCode = event.id.hashCode() // <--- Используем ID вместо summary

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

    val starShape = remember(shapeParams.numVertices, shapeParams.radiusSeed) {
        RoundedPolygon.star(
            numVerticesPerRadius = shapeParams.numVertices,
            radius = shapeParams.radiusSeed,
            rounding = CornerRounding(0.95f)
        )
    }
    val clipStar = remember(starShape) { RoundedPolygonShape(polygon = starShape) }
    val clip2Star = remember(starShape) { RoundedPolygonShape(polygon = starShape) }

    val starContainerSize = CalendarUiDefaults.StarContainerSize
    val starOffsetY = starContainerSize * shapeParams.offestParam
    val starOffsetX = starContainerSize * -shapeParams.offestParam
    val rotationAngle = remember(event.id) { (event.id.hashCode() % 45).toFloat() } // Используем ID
    val shadowColor = Color.Black.copy(alpha = 0.3f)
    val density = LocalDensity.current
    val darkerShadowColor = Color.Black

    // --- Параметры текущего события (получаем isCurrentEvent) ---
    val fixedColors = LocalFixedAccentColors.current
    val cardElevation = if (isCurrentEvent) CalendarUiDefaults.CurrentEventElevation else 0.dp
    val cardBackground = if (isCurrentEvent) fixedColors.primaryFixed else colorScheme.primaryContainer
    val cardTextColor = if (isCurrentEvent) fixedColors.onPrimaryFixed else colorScheme.onPrimaryContainer

    // --- Композиция UI ---
    Box(
        modifier = modifier // Применяем модификатор от родителя
            // Убрали внешний padding отсюда
            .shadow(
                elevation = cardElevation,
                shape = RoundedCornerShape(CalendarUiDefaults.EventItemCornerRadius),
                clip = false,
                ambientColor = if (cardElevation > 0.dp) darkerShadowColor else Color.Transparent,
                spotColor = if (cardElevation > 0.dp) darkerShadowColor else Color.Transparent
            )
            .clip(RoundedCornerShape(CalendarUiDefaults.EventItemCornerRadius))
            .background(cardBackground)
            .height(targetHeight) // Высота применяется здесь
    ) {
        // --- Декорация (оставлена без изменений по запросу) ---
        if (!isMicroEvent) {
            // Тень
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .graphicsLayer(
                        translationX = with(density) { (starOffsetX + shapeParams.shadowOffsetXSeed).toPx() },
                        translationY = with(density) { (starOffsetY - shapeParams.shadowOffsetYSeed).toPx() },
                        rotationZ = rotationAngle,
                        renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            BlurEffect(radiusX = CalendarUiDefaults.StarShadowBlurRadius, radiusY = CalendarUiDefaults.StarShadowBlurRadius, edgeTreatment = TileMode.Decal)
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
                    .background(cardBackground.copy(alpha = 0.95f)) // Используем фон карточки
            )
        }

        // --- Текстовый контент ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Используем константы для внутренних отступов
                .padding(
                    horizontal = CalendarUiDefaults.ItemHorizontalPadding, // Внутренний горизонтальный
                    vertical = if (isMicroEvent) CalendarUiDefaults.MicroItemContentVerticalPadding else CalendarUiDefaults.StandardItemContentVerticalPadding
                ),
            contentAlignment = Alignment.TopStart
        ) {
            if (isMicroEvent) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.summary,
                        color = cardTextColor,
                        style = typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(CalendarUiDefaults.HeaderBottomSpacing))
                    Text(
                        text = timeFormatter(event),
                        color = cardTextColor,
                        style = typography.labelMedium,
                        maxLines = 1
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = event.summary,
                        color = cardTextColor,
                        style = typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timeFormatter(event),
                        color = cardTextColor,
                        style = typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

