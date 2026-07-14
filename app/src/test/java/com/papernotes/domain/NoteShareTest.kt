package com.papernotes.domain

import com.google.common.truth.Truth.assertThat
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.NoteType
import java.time.LocalDate
import org.junit.Test

class NoteShareTest {

    @Test
    fun `Text-Notiz teilt Titel plus Leerzeile plus Body`() {
        val note = Note(title = " Einkauf ", body = " Milch holen ", type = NoteType.TEXT)
        assertThat(note.toShareText()).isEqualTo("Einkauf\n\nMilch holen")
    }

    @Test
    fun `leere Felder werden weggelassen`() {
        assertThat(Note(title = "Nur Titel", type = NoteType.TEXT).toShareText()).isEqualTo("Nur Titel")
        assertThat(Note(body = "Nur Body", type = NoteType.TEXT).toShareText()).isEqualTo("Nur Body")
    }

    @Test
    fun `Checkliste nutzt Kaestchen-Glyphen statt interner Kodierung`() {
        val note = Note(body = "[x] Brot\n[ ] Milch", type = NoteType.CHECKLIST)
        assertThat(note.toShareBody()).isEqualTo("☑ Brot\n☐ Milch")
    }

    @Test
    fun `Stempelkarte teilt Straehne Rekord und Gesamtzahl`() {
        val today = LocalDate.now().toEpochDay()
        val note = Note(
            body = StampCodec.serialize(setOf(today, today - 1)),
            type = NoteType.STAMPCARD,
        )
        assertThat(note.toShareBody()).isEqualTo("Strähne: 2 Tage · Rekord: 2 · 2× gestempelt")
    }

    @Test
    fun `Stempelkarte ohne Stempel laesst Rekord-Zeile weg`() {
        val note = Note(body = "", type = NoteType.STAMPCARD)
        assertThat(note.toShareBody()).isEqualTo("Strähne: 0 Tage · 0× gestempelt")
    }

    @Test
    fun `Skizze teilt Platzhalter`() {
        val note = Note(body = "0.1,0.2 0.3,0.4", type = NoteType.SKETCH)
        assertThat(note.toShareBody()).isEqualTo("🖊️ Skizze")
    }
}
