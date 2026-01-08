package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.core.ui.theme.CaliindaTheme
import com.lpavs.caliinda.core.ui.theme.cuid

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)
@Composable
fun CalendarCreateEventItem(
    modifier: Modifier = Modifier,
    onCreateEventClick: () -> Unit,
) {
    val targetHeight = cuid.MinEventHeight

    val borderColor = colorScheme.outline
    val cardBackground = colorScheme.tertiaryContainer.copy(alpha = 0.5f)

    val cardTextColor = colorScheme.onTertiaryContainer //.copy(alpha = 0.8f)

    // --- Композиция UI ---
    Box( // Корневой Box для тени, фона, высоты и кликабельности
        modifier =
            modifier
                .padding(
                    horizontal = CalendarUiDefaults.ItemHorizontalPadding,
                    vertical = CalendarUiDefaults.ItemVerticalPadding
                )
                .clip(RoundedCornerShape(cuid.EventItemCornerRadius))
                .drawBehind {
                    val radius = cuid.EventItemCornerRadius.toPx()
                    drawRoundRect(
                        color = borderColor,
                        cornerRadius = CornerRadius(radius, radius),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(10f, 10f), 0f
                            )
                        )
                    )
                }
                .background(cardBackground)
                .height(targetHeight)
                .clickable(
                    onClick = onCreateEventClick
                )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier.weight(1f) // Занимает все место, ОСТАВЛЯЯ место для кнопок снизу
                        .fillMaxWidth()
                        // Внутренние отступы для текста и звезды
                        .padding(
                            horizontal = cuid.ItemHorizontalPadding,
                            vertical = cuid.StandardItemContentVerticalPadding
                        ),
                // Выравнивание контента можно оставить TopStart или изменить на Center, если нужно
                contentAlignment = Alignment.Center
            ) {
                Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
//                    Text(
//                        text = "Create event",
//                        color = cardTextColor,
//                        style = textStyle,
//                        fontFamily = cardFontFamily,
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis
//                    )
                    Icon(
                        imageVector = Icons.Filled.AddCircle,
                        contentDescription = "Create event",
                        tint = cardTextColor
                        )
                }
            }
        }
    }
} // Конец корневого Box


@Preview(showBackground = true, wallpaper = Wallpapers.YELLOW_DOMINATED_EXAMPLE)
@Composable
fun CalendarCreateEventPreview() {
  CaliindaTheme {
      CalendarCreateEventItem(
          onCreateEventClick = {}
      )
  }
}
