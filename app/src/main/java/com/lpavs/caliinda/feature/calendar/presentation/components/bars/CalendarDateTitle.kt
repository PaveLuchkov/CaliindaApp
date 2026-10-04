package com.lpavs.caliinda.feature.calendar.presentation.components.bars

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.theme.Typography
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)
@Composable
fun CalendarDateTitle(
    date: LocalDate,
    hasCalendarAccess: Boolean,
    onTitleClick: () -> Unit,
    onTitleHold: () -> Unit
) {
    val isToday = date == LocalDate.now()
    val isCurrentYear = date.year == LocalDate.now().year
    val headerBackgroundColor =
        if (isToday) {
            colorScheme.tertiary
        } else {
            colorScheme.secondary
        }
    val headerTextColor =
        if (isToday) {
            colorScheme.onTertiary
        } else {
            colorScheme.onSecondary
        }
    val headerTextStyle =
        when {
            isToday -> Typography.titleLargeEmphasized
            else -> Typography.titleLarge
        }
    val headerFontFamily =
        when {
            isToday ->
                FontFamily(
                    Font(
                        R.font.robotoflex_variable,
                        variationSettings =
                            FontVariation.Settings(
                                FontVariation.weight(750),
                            )))
            else ->
                FontFamily(
                    Font(
                        R.font.robotoflex_variable,
                        variationSettings =
                            FontVariation.Settings(
                                FontVariation.weight(600),
                            )))
        }
    val currentLocale = LocalConfiguration.current.getLocales().get(0)
    val formatterWithShortDay = if (isCurrentYear) DateTimeFormatter.ofPattern("E, d MMMM", currentLocale) else DateTimeFormatter.ofPattern("E, d MMMM, yyyy", currentLocale)
    val haptic = LocalHapticFeedback.current
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(color = headerBackgroundColor)
                .pointerInput(Unit) {
                    if (hasCalendarAccess) {
                        detectTapGestures(
                            onPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                tryAwaitRelease()
                            },
                            onTap = { onTitleClick() },
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTitleHold()
                            }
                        )
                    }
                },

        ) {
        Text(
            text = date.format(formatterWithShortDay),
            style = headerTextStyle,
            fontFamily = headerFontFamily,
            color = headerTextColor,
            modifier =
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
        )
    }
}
