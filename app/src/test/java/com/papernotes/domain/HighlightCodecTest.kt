package com.papernotes.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HighlightCodecTest {

    @Test
    fun `serialize und parse sind Roundtrip-stabil`() {
        val list = listOf(Highlight(3, 9, 0), Highlight(15, 20, 2))
        assertThat(HighlightCodec.parse(HighlightCodec.serialize(list))).isEqualTo(list)
    }

    @Test
    fun `parse von leerem String ergibt leere Liste`() {
        assertThat(HighlightCodec.parse("")).isEmpty()
        assertThat(HighlightCodec.parse("   ")).isEmpty()
    }

    @Test
    fun `parse verwirft ungueltige Tokens`() {
        assertThat(HighlightCodec.parse("3-9,a-b-c,1-2-3"))
            .containsExactly(Highlight(1, 2, 3))
    }

    @Test
    fun `parse verwirft Bereiche mit end kleiner gleich start`() {
        assertThat(HighlightCodec.parse("5-5-0,9-3-1,2-4-0"))
            .containsExactly(Highlight(2, 4, 0))
    }

    @Test
    fun `serialize sortiert nach start und filtert leere Bereiche`() {
        val list = listOf(Highlight(10, 12, 1), Highlight(2, 4, 0), Highlight(7, 7, 2))
        assertThat(HighlightCodec.serialize(list)).isEqualTo("2-4-0,10-12-1")
    }
}
