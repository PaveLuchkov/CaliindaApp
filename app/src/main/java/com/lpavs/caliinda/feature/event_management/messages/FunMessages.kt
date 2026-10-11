package com.lpavs.caliinda.feature.event_management.messages

import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.utils.UiText
import javax.inject.Inject
import javax.inject.Singleton

interface IFunMessages {
  fun getEventCreatedMessage(eventName: String? = null): UiText

  fun getEventUpdatedMessage(eventName: String? = null): UiText

  fun getEventDeletedMessage(eventName: String? = null): UiText

  fun getSeriesDeletedMessage(): UiText

  fun getCreateErrorMessage(): UiText

  fun getUpdateErrorMessage(): UiText

  fun getDeleteErrorMessage(): UiText
}

// Фраз немного: каждую адаптируем под все языки приложения, а не переводим сленг дословно.
@Singleton
class FunMessagesImpl @Inject constructor() : IFunMessages {

  private val created =
      listOf(
          R.string.event_created_dropped,
          R.string.event_created_baked,
          R.string.event_created_nailed)

  private val createdWithName =
      listOf(
          R.string.event_created_with_name_live,
          R.string.event_created_with_name_entered_chat,
          R.string.event_created_with_name_deployed)

  private val updated =
      listOf(
          R.string.event_updated_glow_up,
          R.string.event_updated_leveled,
          R.string.event_updated_live)

  private val updatedWithName =
      listOf(
          R.string.event_updated_with_name_glow_up,
          R.string.event_updated_with_name_leveled,
          R.string.event_updated_with_name_makeover)

  private val deleted =
      listOf(
          R.string.event_deleted_shadow_realm,
          R.string.event_deleted_left_chat,
          R.string.event_deleted_vanished)

  private val deletedWithName =
      listOf(
          R.string.event_deleted_with_name_shadow_realm,
          R.string.event_deleted_with_name_left_chat,
          R.string.event_deleted_with_name_vanished)

  private val seriesDeleted =
      listOf(R.string.series_deleted_dynasty, R.string.series_deleted_family_tree)

  private val createErrors =
      listOf(R.string.error_create_rejected, R.string.error_create_cook_failed)

  private val updateErrors =
      listOf(R.string.error_update_makeover_wrong, R.string.error_update_refused)

  private val deleteErrors = listOf(R.string.error_delete_immortal, R.string.error_delete_blocked)

  /** Пустое название — фраза без имени, а не захардкоженная заглушка. */
  private fun pick(
      eventName: String?,
      plain: List<Int>,
      withName: List<Int>
  ): UiText {
    val name = eventName?.takeIf { it.isNotBlank() }
    return if (name != null) UiText.from(withName.random(), name) else random(plain)
  }

  private fun random(ids: List<Int>): UiText = UiText.from(ids.random())

  override fun getEventCreatedMessage(eventName: String?) = pick(eventName, created, createdWithName)

  override fun getEventUpdatedMessage(eventName: String?) = pick(eventName, updated, updatedWithName)

  override fun getEventDeletedMessage(eventName: String?) = pick(eventName, deleted, deletedWithName)

  override fun getSeriesDeletedMessage() = random(seriesDeleted)

  override fun getCreateErrorMessage() = random(createErrors)

  override fun getUpdateErrorMessage() = random(updateErrors)

  override fun getDeleteErrorMessage() = random(deleteErrors)
}
