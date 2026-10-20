package com.lpavs.caliinda.core.data.calendar.model

import android.provider.CalendarContract.Attendees
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendeesTest {
  private fun row(
      name: String?,
      email: String?,
      status: Int = Attendees.ATTENDEE_STATUS_NONE,
      relationship: Int = Attendees.RELATIONSHIP_ATTENDEE,
      type: Int = Attendees.TYPE_REQUIRED,
  ) = AttendeeRow(name, email, status, relationship, type)

  @Test
  fun `порядок — организатор, вы, придут, может быть, не ответили, не придут`() {
    val rows =
        listOf(
            row("Боря", "b@x.com", Attendees.ATTENDEE_STATUS_DECLINED),
            row(null, "me@x.com", Attendees.ATTENDEE_STATUS_ACCEPTED),
            row("Вера", "v@x.com", Attendees.ATTENDEE_STATUS_TENTATIVE),
            row("Аня", "a@x.com", Attendees.ATTENDEE_STATUS_ACCEPTED),
            row("Гоша", "g@x.com"),
            row("Олег", "boss@x.com", relationship = Attendees.RELATIONSHIP_ORGANIZER))
    val people = toAttendees(rows, ownerAccount = "Me@X.com", organizer = "boss@x.com")
    assertEquals(listOf("Олег", "me@x.com", "Аня", "Вера", "Гоша", "Боря"), people.map { it.name })
    assertTrue(people[0].isOrganizer)
    assertTrue(people[1].isSelf)
    assertEquals(AttendeeStatus.AWAITING, people[4].status)
  }

  @Test
  fun `переговорки, пустые и дубли отбрасываются`() {
    val rows =
        listOf(
            row("Room 5", "room5@resource.x.com", type = Attendees.TYPE_RESOURCE),
            row(null, null),
            row("Аня", "a@x.com"),
            row("Аня", "A@x.com"))
    assertEquals(listOf("Аня"), toAttendees(rows, "me@x.com", null).map { it.name })
  }

  @Test
  fun `только вы — показывать нечего`() {
    assertTrue(toAttendees(listOf(row("Я", "me@x.com")), "me@x.com", null).isEmpty())
    assertTrue(toAttendees(emptyList(), "me@x.com", null).isEmpty())
  }
}
