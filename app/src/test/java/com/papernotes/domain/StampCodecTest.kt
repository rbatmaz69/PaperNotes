package com.papernotes.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StampCodecTest {

    @Test
    fun `parse ignoriert Motiv-Token und ungueltige Tokens`() {
        assertThat(StampCodec.parse("m1,100,101,quatsch, 102 ")).containsExactly(100L, 101L, 102L)
    }

    @Test
    fun `parse von leerem Body ergibt leere Menge`() {
        assertThat(StampCodec.parse("")).isEmpty()
    }

    @Test
    fun `motif liest Token und faellt sonst auf CHECK zurueck`() {
        assertThat(StampCodec.motif("m1,100")).isEqualTo(StampMotif.STAR)
        assertThat(StampCodec.motif("100,101")).isEqualTo(StampMotif.CHECK)
        assertThat(StampCodec.motif("m99,100")).isEqualTo(StampMotif.CHECK) // out of range
        assertThat(StampCodec.motif("mxyz,100")).isEqualTo(StampMotif.CHECK)
    }

    @Test
    fun `serialize stellt Motiv voran und sortiert Tage`() {
        assertThat(StampCodec.serialize(setOf(300L, 100L, 200L), StampMotif.HEART))
            .isEqualTo("m2,100,200,300")
    }

    @Test
    fun `serialize und parse sind Roundtrip-stabil inkl Motiv`() {
        val days = setOf(5L, 9L, 7L)
        val body = StampCodec.serialize(days, StampMotif.LEAF)
        assertThat(StampCodec.parse(body)).isEqualTo(days)
        assertThat(StampCodec.motif(body)).isEqualTo(StampMotif.LEAF)
    }

    @Test
    fun `streak zaehlt Serie die heute endet`() {
        assertThat(StampCodec.streak(setOf(98L, 99L, 100L), today = 100L)).isEqualTo(3L)
    }

    @Test
    fun `streak lebt noch wenn nur bis gestern gestempelt`() {
        assertThat(StampCodec.streak(setOf(98L, 99L), today = 100L)).isEqualTo(2L)
    }

    @Test
    fun `streak ist 0 bei Luecke oder leerer Menge`() {
        assertThat(StampCodec.streak(setOf(97L, 98L), today = 100L)).isEqualTo(0L)
        assertThat(StampCodec.streak(emptySet(), today = 100L)).isEqualTo(0L)
    }

    @Test
    fun `longestStreak findet laengste Serie in unsortierter Eingabe`() {
        // Serien: 10-11-12 (3) und 20-21 (2)
        assertThat(StampCodec.longestStreak(setOf(21L, 10L, 12L, 20L, 11L))).isEqualTo(3L)
    }

    @Test
    fun `longestStreak fuer Einzeltag und leere Menge`() {
        assertThat(StampCodec.longestStreak(setOf(42L))).isEqualTo(1L)
        assertThat(StampCodec.longestStreak(emptySet())).isEqualTo(0L)
    }
}
