package com.example.caliindar.data.local

import android.util.Log
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateTimeUtils {
    fun parseToInstant(dateTimeString: String?, zoneIdString: String): Instant? {
        if (dateTimeString.isNullOrBlank()) return null
        val zoneId = try { ZoneId.of(zoneIdString.ifEmpty { ZoneId.systemDefault().id }) }
        catch (e: Exception) { ZoneId.systemDefault() }
        return try { OffsetDateTime.parse(dateTimeString).toInstant() }
        catch (e: DateTimeParseException) {
            try { LocalDateTime.parse(dateTimeString).atZone(zoneId).toInstant() }
            catch (e2: DateTimeParseException) { null }
        }
    }

    fun formatMillisToIsoString(millis: Long?, zoneIdString: String): String? {
        if (millis == null) return null
        val zoneId = try { ZoneId.of(zoneIdString.ifEmpty { ZoneId.systemDefault().id }) }
        catch (e: Exception) { ZoneId.systemDefault() }
        return try {
            Instant.ofEpochMilli(millis)
                .atZone(zoneId) // Применяем нужный пояс
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) // Формат со смещением
        } catch (e: Exception) {
            Log.e("DateTimeUtils", "Error formatting millis $millis to ISO string for zone $zoneIdString", e)
            null // Возвращаем null при ошибке
        }
    }
}