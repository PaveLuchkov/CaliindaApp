package com.lpavs.caliinda.ui.screens.main.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.text.format

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalendarAppBar(
    onNavigateToSettings: () -> Unit,
    onGoToTodayClick: () -> Unit,
    onTitleClick: () -> Unit,
    date: LocalDate
) {
    val isToday = date == LocalDate.now()
    val headerBackgroundColor = if (isToday) {
        colorScheme.tertiary
    } else {
        colorScheme.secondary
    }
    val headerTextColor = if (isToday) {
        colorScheme.onTertiary
    } else {
        colorScheme.onSecondary
    }
    val currentLocale = LocalConfiguration.current.getLocales().get(0)
    val formatterWithShortDay = DateTimeFormatter.ofPattern("E, d MMMM yyyy", currentLocale)
    CenterAlignedTopAppBar(
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(color = headerBackgroundColor)
                    .clickable(onClick = onTitleClick),

            ){
                Text(
                    text = date.format(formatterWithShortDay),
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = headerTextColor,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth(),// Больше отступы
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                )
            }
        },
        navigationIcon = {
            FilledIconButton(
                onClick = onGoToTodayClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(
                        IconButtonDefaults.smallContainerSize(
                            IconButtonDefaults.IconButtonWidthOption.Wide
                        )
                    ),
                shape = IconButtonDefaults.smallRoundShape
            ) {
                Icon(
                    Icons.Filled.Today,
                    contentDescription = "Перейти к сегодня",
                )
            }
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(
                            IconButtonDefaults.smallContainerSize(
                                IconButtonDefaults.IconButtonWidthOption.Wide
                            )
                        ),
                    shape = IconButtonDefaults.smallRoundShape
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Настройки",
                    )
                }
            }
        },
        colors = topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}