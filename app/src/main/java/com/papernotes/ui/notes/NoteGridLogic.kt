package com.papernotes.ui.notes

import com.papernotes.domain.model.MoodCategory
import com.papernotes.domain.model.Note
import java.text.Collator
import java.util.Locale

/** Notiz fürs Grid; [dimmed] = Nicht-Treffer von Suche/Stimmungs-Filter ("verdünnte Tinte"). */
data class GridNote(
    val note: Note,
    val dimmed: Boolean,
)

/**
 * Eine Grid-Zelle: entweder eine einzelne Notiz ([SoloItem]) oder ein Büroklammer-Stapel
 * ([StackItem]) aus mehreren zusammengeklammerten Notizen.
 */
sealed interface GridItem {
    val key: String
    val dimmed: Boolean
}

data class SoloItem(val gridNote: GridNote) : GridItem {
    override val key: String get() = "note-${gridNote.note.id}"
    override val dimmed: Boolean get() = gridNote.dimmed
}

/** Stapel: [cover] liegt oben, [members] enthält alle (inkl. Cover) in Sortier-Reihenfolge. */
data class StackItem(
    val clipId: Long,
    val cover: GridNote,
    val members: List<GridNote>,
) : GridItem {
    override val key: String get() = "clip-$clipId"
    override val dimmed: Boolean get() = members.all { it.dimmed }
}

/**
 * Fasst die (bereits sortierten) Grid-Notizen zu Grid-Items zusammen: Notizen mit gleichem
 * `clipId` werden – sofern ≥2 aktiv sichtbar – an der Position der obersten zu einem [StackItem]
 * gebündelt; alle anderen bleiben [SoloItem]. Die ursprüngliche Reihenfolge bleibt erhalten.
 */
internal fun groupIntoItems(notes: List<GridNote>): List<GridItem> {
    val byClip = notes.filter { it.note.clipId != null }.groupBy { it.note.clipId!! }
    val emitted = mutableSetOf<Long>()
    val items = mutableListOf<GridItem>()
    for (gridNote in notes) {
        val clipId = gridNote.note.clipId
        val group = clipId?.let { byClip[it] }
        if (clipId != null && group != null && group.size >= 2) {
            if (emitted.add(clipId)) {
                items += StackItem(clipId = clipId, cover = group.first(), members = group)
            }
        } else {
            items += SoloItem(gridNote)
        }
    }
    return items
}

/**
 * true, wenn die Notiz zu Suche + Stimmungs- + Tag-Filter passt (Nicht-Treffer werden im Grid
 * gedimmt, nicht ausgeblendet). Versiegelte Notizen tauchen nie in Suchtreffern auf (kein
 * Inhalts-Leak), bleiben aber ohne aktive Suche normal sichtbar.
 */
internal fun matchesFilters(
    note: Note,
    trimmedQuery: String,
    mood: MoodCategory?,
    tag: String?,
): Boolean {
    val matchesQuery = trimmedQuery.isEmpty() ||
        (!note.sealed && (
            note.title.contains(trimmedQuery, ignoreCase = true) ||
                note.body.contains(trimmedQuery, ignoreCase = true) ||
                note.backText.contains(trimmedQuery, ignoreCase = true) ||
                note.tagList.any { it.contains(trimmedQuery, ignoreCase = true) }
            ))
    val matchesMood = mood == null || note.mood == mood
    val matchesTag = tag == null || tag in note.tagList
    return matchesQuery && matchesMood && matchesTag
}

/** Sortiert in-memory nach dem DAO-Order (pinned DESC, position ASC, updatedAt DESC). */
internal fun sortNotes(notes: List<Note>, sort: SortMode): List<Note> = when (sort) {
    SortMode.PINNWAND -> notes
    SortMode.CREATED -> notes.sortedWith(
        compareByDescending<Note> { it.pinned }.thenByDescending { it.createdAt },
    )
    SortMode.TITLE -> notes.sortedWith(
        compareByDescending<Note> { it.pinned }
            // Collator statt String-Vergleich, damit Umlaute korrekt einsortieren.
            .then(compareBy(Collator.getInstance(Locale.GERMAN)) { it.title.ifBlank { it.preview } }),
    )
}
