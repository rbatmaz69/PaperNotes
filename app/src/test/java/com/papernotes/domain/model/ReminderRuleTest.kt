package com.papernotes.domain.model

import com.google.common.truth.Truth.assertThat
import java.util.Calendar
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test

class ReminderRuleTest {

    private lateinit var originalZone: TimeZone
    private val berlin = TimeZone.getTimeZone("Europe/Berlin")

    @Before
    fun fixTimeZone() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(berlin)
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalZone)
    }

    /** Epoch-Millis für ein lokales Datum in Europe/Berlin. */
    private fun at(year: Int, month: Int, day: Int, hour: Int = 9, minute: Int = 30): Long =
        Calendar.getInstance(berlin).apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.timeInMillis

    private fun Long.toCal(): Calendar = Calendar.getInstance(berlin).apply { timeInMillis = this@toCal }

    @Test
    fun `NONE gibt from unveraendert zurueck`() {
        val from = at(2026, 7, 14)
        assertThat(ReminderRule.NONE.next(from)).isEqualTo(from)
    }

    @Test
    fun `DAILY plus ein Tag gleiche Uhrzeit`() {
        val next = ReminderRule.DAILY.next(at(2026, 7, 14)).toCal()
        assertThat(next.get(Calendar.DAY_OF_MONTH)).isEqualTo(15)
        assertThat(next.get(Calendar.HOUR_OF_DAY)).isEqualTo(9)
        assertThat(next.get(Calendar.MINUTE)).isEqualTo(30)
    }

    @Test
    fun `WEEKLY plus sieben Tage gleiche Uhrzeit`() {
        val next = ReminderRule.WEEKLY.next(at(2026, 7, 14)).toCal()
        assertThat(next.get(Calendar.DAY_OF_MONTH)).isEqualTo(21)
        assertThat(next.get(Calendar.HOUR_OF_DAY)).isEqualTo(9)
    }

    @Test
    fun `WEEKDAYS ueberspringt Wochenende von Freitag auf Montag`() {
        // 2026-07-17 ist ein Freitag
        val next = ReminderRule.WEEKDAYS.next(at(2026, 7, 17)).toCal()
        assertThat(next.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.MONDAY)
        assertThat(next.get(Calendar.DAY_OF_MONTH)).isEqualTo(20)
    }

    @Test
    fun `WEEKDAYS von Samstag auf Montag`() {
        // 2026-07-18 ist ein Samstag
        val next = ReminderRule.WEEKDAYS.next(at(2026, 7, 18)).toCal()
        assertThat(next.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.MONDAY)
        assertThat(next.get(Calendar.DAY_OF_MONTH)).isEqualTo(20)
    }

    @Test
    fun `WEEKDAYS mitten in der Woche plus ein Tag`() {
        // 2026-07-15 ist ein Mittwoch
        val next = ReminderRule.WEEKDAYS.next(at(2026, 7, 15)).toCal()
        assertThat(next.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.THURSDAY)
        assertThat(next.get(Calendar.DAY_OF_MONTH)).isEqualTo(16)
    }

    @Test
    fun `DAILY ueber den Jahreswechsel`() {
        val next = ReminderRule.DAILY.next(at(2026, 12, 31)).toCal()
        assertThat(next.get(Calendar.YEAR)).isEqualTo(2027)
        assertThat(next.get(Calendar.MONTH)).isEqualTo(Calendar.JANUARY)
        assertThat(next.get(Calendar.DAY_OF_MONTH)).isEqualTo(1)
    }

    @Test
    fun `DAILY behaelt die Wanduhrzeit ueber die Sommerzeit-Umstellung`() {
        // Nacht 28.→29. März 2026: Europe/Berlin springt von 02:00 auf 03:00
        val next = ReminderRule.DAILY.next(at(2026, 3, 28)).toCal()
        assertThat(next.get(Calendar.DAY_OF_MONTH)).isEqualTo(29)
        assertThat(next.get(Calendar.HOUR_OF_DAY)).isEqualTo(9)
        assertThat(next.get(Calendar.MINUTE)).isEqualTo(30)
    }

    @Test
    fun `fromName mit null oder Unbekanntem ergibt NONE`() {
        assertThat(ReminderRule.fromName(null)).isEqualTo(ReminderRule.NONE)
        assertThat(ReminderRule.fromName("QUATSCH")).isEqualTo(ReminderRule.NONE)
        assertThat(ReminderRule.fromName("WEEKLY")).isEqualTo(ReminderRule.WEEKLY)
    }
}
