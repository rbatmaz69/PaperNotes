package com.papernotes.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SketchCodecTest {

    @Test
    fun `serialize und parse sind Roundtrip-stabil mit Farbe und Breite`() {
        val strokes = listOf(
            SketchStroke(
                points = listOf(SketchPoint(0.1f, 0.2f), SketchPoint(0.5f, 0.75f)),
                color = 0xFF0000,
                width = 4.5f,
            ),
        )
        assertThat(SketchCodec.parse(SketchCodec.serialize(strokes))).isEqualTo(strokes)
    }

    @Test
    fun `Legacy-Strich ohne Kopf hat weder Farbe noch Breite`() {
        val strokes = SketchCodec.parse("0.1,0.2 0.3,0.4")
        assertThat(strokes).hasSize(1)
        assertThat(strokes[0].color).isNull()
        assertThat(strokes[0].width).isNull()
        assertThat(strokes[0].points).containsExactly(SketchPoint(0.1f, 0.2f), SketchPoint(0.3f, 0.4f)).inOrder()
    }

    @Test
    fun `Strich ohne Farbe und Breite wird Legacy-kompatibel serialisiert`() {
        val strokes = listOf(SketchStroke(listOf(SketchPoint(0.5f, 0.5f))))
        assertThat(SketchCodec.serialize(strokes)).doesNotContain("|")
    }

    @Test
    fun `parse ueberspringt leere Segmente zwischen Semikolons`() {
        val strokes = SketchCodec.parse("0.1,0.1;;0.2,0.2; ;")
        assertThat(strokes).hasSize(2)
    }

    @Test
    fun `parse verwirft kaputte Punkt-Tokens aber behaelt gueltige`() {
        val strokes = SketchCodec.parse("0.1,0.2 kaputt 0.3 0.4,0.5")
        assertThat(strokes).hasSize(1)
        assertThat(strokes[0].points).hasSize(2)
    }

    @Test
    fun `parse mit kaputtem Meta-Kopf liest Punkte trotzdem`() {
        val strokes = SketchCodec.parse("zzz|0.1,0.2")
        assertThat(strokes).hasSize(1)
        assertThat(strokes[0].color).isNull()
        assertThat(strokes[0].points).containsExactly(SketchPoint(0.1f, 0.2f))
    }

    @Test
    fun `serialize filtert Striche ohne Punkte`() {
        val strokes = listOf(SketchStroke(emptyList()), SketchStroke(listOf(SketchPoint(0.1f, 0.1f))))
        assertThat(SketchCodec.parse(SketchCodec.serialize(strokes))).hasSize(1)
    }

    @Test
    fun `mehrere Striche bleiben getrennt und geordnet`() {
        val strokes = listOf(
            SketchStroke(listOf(SketchPoint(0.1f, 0.1f)), color = 0x00FF00, width = 2f),
            SketchStroke(listOf(SketchPoint(0.9f, 0.9f))),
        )
        assertThat(SketchCodec.parse(SketchCodec.serialize(strokes))).isEqualTo(strokes)
    }
}
