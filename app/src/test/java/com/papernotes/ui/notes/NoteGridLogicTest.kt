package com.papernotes.ui.notes

import com.google.common.truth.Truth.assertThat
import com.papernotes.domain.model.MoodCategory
import com.papernotes.domain.model.Note
import org.junit.Test

class NoteGridLogicTest {

    private fun grid(vararg notes: Note) = notes.map { GridNote(it, dimmed = false) }

    // --- groupIntoItems ---

    @Test
    fun `zwei Notizen mit gleichem Clip werden zum Stapel an Position der ersten`() {
        val a = Note(id = 1, clipId = 7L)
        val b = Note(id = 2)
        val c = Note(id = 3, clipId = 7L)
        val items = groupIntoItems(grid(a, b, c))

        assertThat(items).hasSize(2)
        val stack = items[0] as StackItem
        assertThat(stack.clipId).isEqualTo(7L)
        assertThat(stack.cover.note.id).isEqualTo(1L)
        assertThat(stack.members.map { it.note.id }).containsExactly(1L, 3L).inOrder()
        assertThat((items[1] as SoloItem).gridNote.note.id).isEqualTo(2L)
    }

    @Test
    fun `einzelnes Clip-Mitglied bleibt Solo`() {
        val items = groupIntoItems(grid(Note(id = 1, clipId = 7L), Note(id = 2)))
        assertThat(items).hasSize(2)
        assertThat(items[0]).isInstanceOf(SoloItem::class.java)
    }

    @Test
    fun `Stapel ist nur gedimmt wenn alle Mitglieder gedimmt sind`() {
        val bright = GridNote(Note(id = 1, clipId = 7L), dimmed = true)
        val dimmed = GridNote(Note(id = 2, clipId = 7L), dimmed = false)
        assertThat(groupIntoItems(listOf(bright, dimmed))[0].dimmed).isFalse()

        val allDim = listOf(
            GridNote(Note(id = 1, clipId = 7L), dimmed = true),
            GridNote(Note(id = 2, clipId = 7L), dimmed = true),
        )
        assertThat(groupIntoItems(allDim)[0].dimmed).isTrue()
    }

    // --- matchesFilters ---

    @Test
    fun `versiegelte Notizen matchen bei aktiver Suche nie`() {
        val secret = Note(title = "Geheim", body = "Geheim", backText = "Geheim", sealed = true)
        assertThat(matchesFilters(secret, "geheim", mood = null, tag = null)).isFalse()
        // Ohne aktive Suche bleibt sie sichtbar (kein Dimmen).
        assertThat(matchesFilters(secret, "", mood = null, tag = null)).isTrue()
    }

    @Test
    fun `Suche ist case-insensitive ueber Titel Body Rueckseite und Tags`() {
        assertThat(matchesFilters(Note(title = "Einkauf"), "EINKAUF", null, null)).isTrue()
        assertThat(matchesFilters(Note(body = "Milch"), "milch", null, null)).isTrue()
        assertThat(matchesFilters(Note(backText = "Rückseite"), "rück", null, null)).isTrue()
        assertThat(matchesFilters(Note(tags = "Arbeit\nPrivat"), "arbeit", null, null)).isTrue()
        assertThat(matchesFilters(Note(title = "anders"), "einkauf", null, null)).isFalse()
    }

    @Test
    fun `Mood-Filter matcht nur die gewaehlte Stimmung`() {
        val joyful = Note(mood = MoodCategory.entries.first { it != MoodCategory.PLAIN })
        assertThat(matchesFilters(joyful, "", mood = joyful.mood, tag = null)).isTrue()
        assertThat(matchesFilters(Note(mood = MoodCategory.PLAIN), "", mood = joyful.mood, tag = null)).isFalse()
    }

    @Test
    fun `Tag-Filter verlangt exakten Reiter`() {
        val tagged = Note(tags = "Arbeit\nPrivat")
        assertThat(matchesFilters(tagged, "", mood = null, tag = "Arbeit")).isTrue()
        assertThat(matchesFilters(tagged, "", mood = null, tag = "Arb")).isFalse()
    }

    // --- sortNotes ---

    @Test
    fun `PINNWAND behaelt die Eingabe-Reihenfolge`() {
        val notes = listOf(Note(id = 3), Note(id = 1), Note(id = 2))
        assertThat(sortNotes(notes, SortMode.PINNWAND)).isEqualTo(notes)
    }

    @Test
    fun `CREATED sortiert gepinnte zuerst dann neueste`() {
        val old = Note(id = 1, createdAt = 100)
        val new = Note(id = 2, createdAt = 200)
        val pinnedOld = Note(id = 3, createdAt = 50, pinned = true)
        assertThat(sortNotes(listOf(old, new, pinnedOld), SortMode.CREATED).map { it.id })
            .containsExactly(3L, 2L, 1L).inOrder()
    }

    @Test
    fun `TITLE sortiert Umlaute mit deutschem Collator korrekt ein`() {
        val a = Note(id = 1, title = "Äpfel")
        val z = Note(id = 2, title = "Zebra")
        val b = Note(id = 3, title = "Birne")
        assertThat(sortNotes(listOf(z, b, a), SortMode.TITLE).map { it.title })
            .containsExactly("Äpfel", "Birne", "Zebra").inOrder()
    }

    @Test
    fun `TITLE faellt bei leerem Titel auf die Vorschau zurueck`() {
        val untitled = Note(id = 1, title = "", body = "Aal")
        val titled = Note(id = 2, title = "Zebra")
        assertThat(sortNotes(listOf(titled, untitled), SortMode.TITLE).map { it.id })
            .containsExactly(1L, 2L).inOrder()
    }
}
