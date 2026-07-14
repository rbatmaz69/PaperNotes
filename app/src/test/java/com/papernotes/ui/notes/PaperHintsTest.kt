package com.papernotes.ui.notes

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PaperHintsTest {

    @Test
    fun `vor dem dritten Start kommt kein Tipp`() {
        assertThat(nextHint(opens = 1, seen = emptySet())).isNull()
        assertThat(nextHint(opens = 2, seen = emptySet())).isNull()
    }

    @Test
    fun `ab dem dritten Start kommt der Langdruck-Tipp zuerst`() {
        assertThat(nextHint(opens = 3, seen = emptySet())).isEqualTo(PaperHint.LONG_PRESS)
    }

    @Test
    fun `gesehene Tipps werden übersprungen, der nächste wartet seinen Start ab`() {
        val seen = setOf(PaperHint.LONG_PRESS.key)
        // Eselsohr-Tipp ist erst ab Start 5 fällig (3 + 2*1).
        assertThat(nextHint(opens = 3, seen = seen)).isNull()
        assertThat(nextHint(opens = 4, seen = seen)).isNull()
        assertThat(nextHint(opens = 5, seen = seen)).isEqualTo(PaperHint.DOG_EAR)
    }

    @Test
    fun `Reihenfolge und Staffelung über alle Tipps`() {
        val seen = mutableSetOf<String>()
        val shown = mutableListOf<PaperHint>()
        for (opens in 1..12) {
            nextHint(opens, seen)?.let { hint ->
                shown += hint
                seen += hint.key
            }
        }
        assertThat(shown).containsExactly(
            PaperHint.LONG_PRESS,
            PaperHint.DOG_EAR,
            PaperHint.PINCH_COLUMNS,
            PaperHint.BACK_FLIP,
            PaperHint.TEABAG,
        ).inOrder()
    }

    @Test
    fun `selbst entdeckte Tipps verschieben die späteren nicht nach vorne`() {
        // Nutzer hat den Langdruck selbst gefunden: Eselsohr bleibt bei Start 5 fällig.
        val seen = setOf(PaperHint.LONG_PRESS.key)
        assertThat(nextHint(opens = 3, seen = seen)).isNull()
        assertThat(nextHint(opens = 5, seen = seen)).isEqualTo(PaperHint.DOG_EAR)
    }

    @Test
    fun `alles gesehen ergibt null`() {
        val all = PaperHint.entries.mapTo(mutableSetOf()) { it.key }
        assertThat(nextHint(opens = 100, seen = all)).isNull()
    }
}
