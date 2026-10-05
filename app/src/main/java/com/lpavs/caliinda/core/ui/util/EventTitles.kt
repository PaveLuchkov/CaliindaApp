package com.lpavs.caliinda.core.ui.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.utils.UiText

// Заглушку «(Без названия)» подставляем только при показе: в данных название остаётся пустым,
// иначе её легко случайно сохранить как настоящее название.

fun EventDto.displayTitle(context: Context): String =
    if (isUntitled) context.getString(R.string.no_title) else summary

@Composable
fun EventDto.displayTitle(): String = if (isUntitled) stringResource(R.string.no_title) else summary

val EventDto.titleText: UiText
  get() = if (isUntitled) UiText.from(R.string.no_title) else UiText.from(summary)
