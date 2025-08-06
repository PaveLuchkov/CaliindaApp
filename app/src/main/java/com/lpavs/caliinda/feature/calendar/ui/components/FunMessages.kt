package com.lpavs.caliinda.feature.calendar.ui.components

import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.utils.UiText
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

interface IFunMessages {
    fun resetSession()
    fun getEventCreatedMessage(eventName: String? = null): UiText
    fun getEventUpdatedMessage(eventName: String? = null): UiText
    fun getEventDeletedMessage(eventName: String? = null): UiText
    fun getSeriesDeletedMessage(): UiText
    fun getGenericErrorMessage(): UiText
    fun getCreateErrorMessage(): UiText
    fun getUpdateErrorMessage(): UiText
    fun getDeleteErrorMessage(): UiText
    fun getSeasonalMessage(): UiText
}

@Singleton
class FunMessagesImpl @Inject constructor() : IFunMessages {

    private var isFirstMessageOfSession = true
    private var lastSessionTimestamp = 0L
    private val SESSION_TIMEOUT_MS = 30 * 60 * 1000L // 30 минут

    override fun resetSession() {
        isFirstMessageOfSession = true
        lastSessionTimestamp = System.currentTimeMillis()
    }

    private fun checkSessionTimeout() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSessionTimestamp > SESSION_TIMEOUT_MS) {
            isFirstMessageOfSession = true
        }
        lastSessionTimestamp = currentTime
    }

    private val eventCreatedMessages =
        listOf(
            R.string.event_created_spawned,
            R.string.event_created_dropped,
            R.string.event_created_deployed,
            R.string.event_created_baked,
            R.string.event_created_nailed
        )

    private val eventUpdatedMessages =
        listOf(
            R.string.event_updated_glow_up,
            R.string.event_updated_upgraded,
            R.string.event_updated_makeover,
            R.string.event_updated_leveled,
            R.string.event_updated_live
        )

    private val eventDeletedMessages =
        listOf(
            R.string.event_deleted_slayed,
            R.string.event_deleted_shadow_realm,
            R.string.event_deleted_left_chat,
            R.string.event_deleted_yeeted,
            R.string.event_deleted_vanished
        )

    private val seriesDeletedMessages =
        listOf(
            R.string.series_deleted_ace,
            R.string.series_deleted_bloodline,
            R.string.series_deleted_dynasty,
            R.string.series_deleted_nuked,
            R.string.series_deleted_family_tree
        )

    private val eventCreatedWithNameMessages =
        listOf(
            R.string.event_created_with_name_spawned,
            R.string.event_created_with_name_deployed,
            R.string.event_created_with_name_live,
            R.string.event_created_with_name_complete,
            R.string.event_created_with_name_entered_chat
        )

    private val eventUpdatedWithNameMessages =
        listOf(
            R.string.event_updated_with_name_glow_up,
            R.string.event_updated_with_name_upgraded,
            R.string.event_updated_with_name_makeover,
            R.string.event_updated_with_name_leveled,
            R.string.event_updated_with_name_live
        )

    private val eventDeletedWithNameMessages =
        listOf(
            R.string.event_deleted_with_name_slayed,
            R.string.event_deleted_with_name_shadow_realm,
            R.string.event_deleted_with_name_left_chat,
            R.string.event_deleted_with_name_yeeted,
            R.string.event_deleted_with_name_vanished
        )

    private val genericErrorMessages =
        listOf(
            R.string.error_generic_idk,
            R.string.error_generic_oopsie,
            R.string.error_generic_sus,
            R.string.error_generic_mission_failed,
            R.string.error_generic_motivation_404,
            R.string.error_generic_spaghetti,
            R.string.error_generic_calendar_gods,
            R.string.error_generic_houston,
            R.string.error_generic_task_failed,
            R.string.error_generic_computer_says_no
        )

    private val createErrorMessages =
        listOf(
            R.string.error_create_spectacular,
            R.string.error_create_rejected,
            R.string.error_create_spawning,
            R.string.error_create_cook_failed
        )

    private val updateErrorMessages =
        listOf(
            R.string.error_update_makeover_wrong,
            R.string.error_update_mission_aborted,
            R.string.error_update_refused,
            R.string.error_update_glow_up_failed
        )

    private val deleteErrorMessages =
        listOf(
            R.string.error_delete_refused_slayed,
            R.string.error_delete_blocked,
            R.string.error_delete_immortal,
            R.string.error_delete_rights_revoked
        )

    private fun getEventCreatedMessageWithTime(eventName: String? = null): UiText {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        return when {
            eventName != null -> {
                when (hour) {
                    in 6..11 -> UiText.from(R.string.morning_event_created, eventName)
                    in 12..17 -> UiText.from(R.string.afternoon_event_created, eventName)
                    in 18..22 -> UiText.from(R.string.evening_event_created, eventName)
                    else -> UiText.from(R.string.night_event_created, eventName)
                }
            }
            else -> {
                when (hour) {
                    in 6..11 -> UiText.from(R.string.morning_event_created_simple)
                    in 12..17 -> UiText.from(R.string.afternoon_event_created_simple)
                    in 18..22 -> UiText.from(R.string.evening_event_created_simple)
                    else -> UiText.from(R.string.night_event_created_simple)
                }
            }
        }
    }

    private fun getEventUpdatedMessageWithTime(eventName: String? = null): UiText {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        return when {
            eventName != null -> {
                when (hour) {
                    in 6..11 -> UiText.from(R.string.morning_event_updated, eventName)
                    in 12..17 -> UiText.from(R.string.afternoon_event_updated, eventName)
                    in 18..22 -> UiText.from(R.string.evening_event_updated, eventName)
                    else -> UiText.from(R.string.night_event_updated, eventName)
                }
            }
            else -> {
                when (hour) {
                    in 6..11 -> UiText.from(R.string.morning_event_updated_simple)
                    in 12..17 -> UiText.from(R.string.afternoon_event_updated_simple)
                    in 18..22 -> UiText.from(R.string.evening_event_updated_simple)
                    else -> UiText.from(R.string.night_event_updated_simple)
                }
            }
        }
    }

    private fun getEventDeletedMessageWithTime(eventName: String? = null): UiText {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        return when {
            eventName != null -> {
                when (hour) {
                    in 6..11 -> UiText.from(R.string.morning_event_deleted, eventName)
                    in 12..17 -> UiText.from(R.string.afternoon_event_deleted, eventName)
                    in 18..22 -> UiText.from(R.string.evening_event_deleted, eventName)
                    else -> UiText.from(R.string.night_event_deleted, eventName)
                }
            }
            else -> {
                when (hour) {
                    in 6..11 -> UiText.from(R.string.morning_event_deleted_simple)
                    in 12..17 -> UiText.from(R.string.afternoon_event_deleted_simple)
                    in 18..22 -> UiText.from(R.string.evening_event_deleted_simple)
                    else -> UiText.from(R.string.night_event_deleted_simple)
                }
            }
        }
    }

    override fun getEventCreatedMessage(eventName: String?): UiText {
        checkSessionTimeout()

        return if (isFirstMessageOfSession) {
            isFirstMessageOfSession = false
            getEventCreatedMessageWithTime(eventName)
        } else {
            if (eventName != null) {
                val safeEventName = eventName.takeIf { it.isNotBlank() } ?: "Untitled Event"
                UiText.from(eventCreatedWithNameMessages.random(), safeEventName)
            } else {
                UiText.from(eventCreatedMessages.random())
            }
        }
    }

    override fun getEventUpdatedMessage(eventName: String?): UiText {
        checkSessionTimeout()

        return if (isFirstMessageOfSession) {
            isFirstMessageOfSession = false
            getEventUpdatedMessageWithTime(eventName)
        } else {
            if (eventName != null) {
                val safeEventName = eventName.takeIf { it.isNotBlank() } ?: "Event"
                UiText.from(eventUpdatedWithNameMessages.random(), safeEventName)
            } else {
                UiText.from(eventUpdatedMessages.random())
            }
        }
    }

    override fun getEventDeletedMessage(eventName: String?): UiText {
        checkSessionTimeout()

        return if (isFirstMessageOfSession) {
            isFirstMessageOfSession = false
            getEventDeletedMessageWithTime(eventName)
        } else {
            if (eventName != null) {
                val safeEventName = eventName.takeIf { it.isNotBlank() } ?: "Event"
                UiText.from(eventDeletedWithNameMessages.random(), safeEventName)
            } else {
                UiText.from(eventDeletedMessages.random())
            }
        }
    }

    override fun getSeriesDeletedMessage(): UiText = UiText.from(seriesDeletedMessages.random())

    override fun getGenericErrorMessage(): UiText = UiText.from(genericErrorMessages.random())

    override fun getCreateErrorMessage(): UiText = UiText.from(createErrorMessages.random())

    override fun getUpdateErrorMessage(): UiText = UiText.from(updateErrorMessages.random())

    override fun getDeleteErrorMessage(): UiText = UiText.from(deleteErrorMessages.random())

    override fun getSeasonalMessage(): UiText {
        val month = Calendar.getInstance().get(Calendar.MONTH)
        return when (month) {
            11, 0, 1 ->
                UiText.from(
                    listOf(
                        R.string.seasonal_winter_magic,
                        R.string.seasonal_ho_ho_ho,
                        R.string.seasonal_frosty_success
                    ).random()
                )
            2, 3, 4 ->
                UiText.from(
                    listOf(
                        R.string.seasonal_spring_vibes,
                        R.string.seasonal_fresh_start,
                        R.string.seasonal_blooming_success
                    ).random()
                )
            5, 6, 7 ->
                UiText.from(
                    listOf(
                        R.string.seasonal_summer_heat,
                        R.string.seasonal_beach_mode,
                        R.string.seasonal_sweet_success
                    ).random()
                )
            else ->
                UiText.from(
                    listOf(
                        R.string.seasonal_autumn_magic,
                        R.string.seasonal_spooky_success,
                        R.string.seasonal_fall_vibes
                    ).random()
                )
        }
    }
}