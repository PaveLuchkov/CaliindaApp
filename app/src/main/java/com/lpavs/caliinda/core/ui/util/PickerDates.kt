package com.lpavs.caliinda.core.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/*
 * M3 DatePicker хранит выбранный день как полночь по UTC. Пояс пользователя сюда подмешивать
 * нельзя: в UTC+ подсветится вчерашний день, в UTC− выбор сохранится на день раньше.
 */

/** Дата → millis для DatePickerState. */
fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Millis из DatePickerState → дата. */
fun Long.fromPickerMillis(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
