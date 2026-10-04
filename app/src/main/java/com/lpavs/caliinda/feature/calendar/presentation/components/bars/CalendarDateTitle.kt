package com.lpavs.caliinda.feature.calendar.presentation.components.bars

import java.time.YearMonth
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.core.ui.theme.CaliindaFonts

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
    today: LocalDate,
    /** На экране проектов вместо даты показываем месяц. */
    month: YearMonth? = null,
    hasCalendarAccess: Boolean,
    onTitleClick: () -> Unit,
    onTitleHold: () -> Unit
) {
    val isToday = if (month != null) month == YearMonth.from(today) else date == today
    val isCurrentYear = date.year == today.year
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
                CaliindaFonts.Heavy
            else ->
                CaliindaFonts.SemiBold
        }
    val currentLocale = LocalConfiguration.current.getLocales().get(0)
    val formatterWithShortDay = if (isCurrentYear) DateTimeFormatter.ofPattern("E, d MMMM", currentLocale) else DateTimeFormatter.ofPattern("E, d MMMM, yyyy", currentLocale)
    val haptic = LocalHapticFeedback.current
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(cuid.ContainerCornerRadius))
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
            text =
                if (month != null) {
                  val pattern = if (month.year == today.year) "LLLL" else "LLLL yyyy"
                  month.format(DateTimeFormatter.ofPattern(pattern, currentLocale))
                      .replaceFirstChar { it.titlecase(currentLocale) }
                } else {
                  date.format(formatterWithShortDay)
                },
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
