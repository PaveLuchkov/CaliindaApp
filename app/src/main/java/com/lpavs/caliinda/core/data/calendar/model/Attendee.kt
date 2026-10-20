package com.lpavs.caliinda.core.data.calendar.model

import android.provider.CalendarContract.Attendees

/** Ответ участника на приглашение. */
enum class AttendeeStatus {
  ACCEPTED,
  TENTATIVE,
  /** Ещё не ответил (или провайдер не знает). */
  AWAITING,
  DECLINED,
}

data class Attendee(
    /** Имя, а если его нет — почта. */
    val name: String,
    val email: String?,
    val status: AttendeeStatus,
    val isOrganizer: Boolean,
    /** Владелец календаря — показываем как «Вы». */
    val isSelf: Boolean,
)

/** Строка таблицы участников в сыром виде — коды провайдера. */
data class AttendeeRow(
    val name: String?,
    val email: String?,
    val status: Int,
    val relationship: Int,
    val type: Int,
)

/**
 * Участники для показа: без переговорок и прочих ресурсов (это не люди), без дублей по почте.
 * Порядок — организатор, вы, затем придут → может быть → не ответили → не придут, внутри — по имени.
 * Если в событии никого, кроме вас, — пусто: показывать нечего.
 */
fun toAttendees(rows: List<AttendeeRow>, ownerAccount: String?, organizer: String?): List<Attendee> {
  val owner = ownerAccount?.lowercase()
  val organizerEmail = organizer?.lowercase()
  val people =
      rows
          .filter { it.type != Attendees.TYPE_RESOURCE }
          .filter { !it.name.isNullOrBlank() || !it.email.isNullOrBlank() }
          .distinctBy { it.email?.lowercase() ?: it.name }
          .map { row ->
            val email = row.email?.takeIf { it.isNotBlank() }
            Attendee(
                name = row.name?.takeIf { it.isNotBlank() } ?: email.orEmpty(),
                email = email,
                status =
                    when (row.status) {
                      Attendees.ATTENDEE_STATUS_ACCEPTED -> AttendeeStatus.ACCEPTED
                      Attendees.ATTENDEE_STATUS_TENTATIVE -> AttendeeStatus.TENTATIVE
                      Attendees.ATTENDEE_STATUS_DECLINED -> AttendeeStatus.DECLINED
                      else -> AttendeeStatus.AWAITING
                    },
                isOrganizer =
                    row.relationship == Attendees.RELATIONSHIP_ORGANIZER ||
                        (email != null && email.lowercase() == organizerEmail),
                isSelf = email != null && email.lowercase() == owner)
          }
  if (people.none { !it.isSelf }) return emptyList()
  return people.sortedWith(
      compareByDescending<Attendee> { it.isOrganizer }
          .thenByDescending { it.isSelf }
          .thenBy { it.status.ordinal }
          .thenBy { it.name.lowercase() })
}
