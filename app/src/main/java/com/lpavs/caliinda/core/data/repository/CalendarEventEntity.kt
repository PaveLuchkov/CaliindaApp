package com.lpavs.caliinda.core.data.repository

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: String,
    val summary: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val description: String?,
    val location: String?,
    val isAllDay: Boolean = false,
    val recurringEventId: String? = null,
    val originalStartTimeString: String? = null,
    val lastFetchedMillis: Long = System.currentTimeMillis(),
    val recurrenceRuleString: String? = null,
    val isPhantom: Boolean = false
)


data class RecurrenceRule(
    val freq: String? = null,
    val until: String? = null,
    val count: Int? = null,
    val interval: Int? = null,
    val byDay: List<String>? = null,
    val byMonthDay: List<Int>? = null,
    val wkst: String? = "MO"
) {
    fun buildString(): String {
        return buildList {
            freq?.let { add("FREQ=$it") }
            until?.let { add("UNTIL=$it") }
            count?.let { add("COUNT=$it") }
            interval?.let { add("INTERVAL=$it") }
            byDay?.takeIf { it.isNotEmpty() }?.let { add("BYDAY=${it.joinToString(",")}") }
            byMonthDay?.takeIf { it.isNotEmpty() }?.let { add("BYMONTHDAY=${it.joinToString(",")}") }
            wkst?.let { add("WKST=$it") }
        }.joinToString(";")
    }

    companion object {
        fun parse(rrule: String?): RecurrenceRule? {
            if (rrule.isNullOrBlank()) return null
            val cleanRule = rrule.removePrefix("RRULE:")
            val parts = cleanRule.split(";").associate {
                val pair = it.split("=")
                pair[0].uppercase() to (pair.getOrNull(1) ?: "")
            }

            return RecurrenceRule(
                freq = parts["FREQ"],
                until = parts["UNTIL"],
                count = parts["COUNT"]?.toIntOrNull(),
                interval = parts["INTERVAL"]?.toIntOrNull(),
                byDay = parts["BYDAY"]?.split(","),
                byMonthDay = parts["BYMONTHDAY"]?.split(",")?.mapNotNull { it.toIntOrNull() }
            )
        }
    }
}