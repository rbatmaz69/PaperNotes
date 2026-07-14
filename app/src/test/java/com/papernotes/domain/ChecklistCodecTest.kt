package com.papernotes.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ChecklistCodecTest {

    @Test
    fun `parse liest offene und erledigte Eintraege`() {
        val items = ChecklistCodec.parse("[ ] Milch\n[x] Brot")
        assertThat(items).containsExactly(
            ChecklistItem("Milch", checked = false),
            ChecklistItem("Brot", checked = true),
        ).inOrder()
    }

    @Test
    fun `parse ist tolerant gegenueber Zeilen ohne Praefix`() {
        val items = ChecklistCodec.parse("von Hand editiert")
        assertThat(items).containsExactly(ChecklistItem("von Hand editiert", checked = false))
    }

    @Test
    fun `parse filtert Leerzeilen`() {
        val items = ChecklistCodec.parse("[ ] a\n\n   \n[x] b")
        assertThat(items).hasSize(2)
    }

    @Test
    fun `serialize und parse sind Roundtrip-stabil`() {
        val items = listOf(
            ChecklistItem("Milch", checked = false),
            ChecklistItem("Brot", checked = true),
            ChecklistItem("mit [x] im Text", checked = false),
        )
        assertThat(ChecklistCodec.parse(ChecklistCodec.serialize(items))).isEqualTo(items)
    }

    @Test
    fun `progress zaehlt erledigte und Gesamtanzahl`() {
        val items = ChecklistCodec.parse("[x] a\n[ ] b\n[x] c")
        assertThat(ChecklistCodec.progress(items)).isEqualTo(2 to 3)
    }

    @Test
    fun `allDone ist false fuer leere Liste`() {
        assertThat(ChecklistCodec.allDone(emptyList())).isFalse()
    }

    @Test
    fun `allDone nur wenn alle erledigt`() {
        val done = ChecklistCodec.parse("[x] a\n[x] b")
        val open = ChecklistCodec.parse("[x] a\n[ ] b")
        assertThat(ChecklistCodec.allDone(done)).isTrue()
        assertThat(ChecklistCodec.allDone(open)).isFalse()
    }
}
