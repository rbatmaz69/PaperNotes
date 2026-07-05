package com.papernotes.domain

import com.papernotes.domain.model.Note
import com.papernotes.domain.model.NoteType
import java.time.LocalDate

private const val DONE_GLYPH = "☑" // ☑
private const val OPEN_GLYPH = "☐" // ☐

/**
 * Wandelt eine Notiz in einen schön lesbaren Klartext zum Teilen um (Papierflieger ✈️).
 *
 * Text-Notizen werden als Titel + Leerzeile + Fließtext exportiert, Checklisten mit
 * hübschen Kästchen-Glyphen (☑/☐) statt der internen `[x]`/`[ ]`-Kodierung
 * (siehe [com.papernotes.domain.ChecklistCodec]). Leere Felder werden weggelassen.
 */
fun Note.toShareText(): String =
    listOf(title.trim(), toShareBody())
        .filter { it.isNotBlank() }
        .joinToString("\n\n")
        .trim()

/** Nur der Inhalt (ohne Titel) als lesbarer Text – für die Karten-Darstellung beim Teilen. */
fun Note.toShareBody(): String = when (type) {
    NoteType.CHECKLIST ->
        checklist.joinToString("\n") { item ->
            (if (item.checked) DONE_GLYPH else OPEN_GLYPH) + " " + item.text
        }
    NoteType.STAMPCARD -> {
        val today = LocalDate.now().toEpochDay()
        val record = StampCodec.longestStreak(stamps)
        buildList {
            add("Strähne: ${StampCodec.streak(stamps, today)} Tage")
            if (record > 0) add("Rekord: $record")
            add("${StampCodec.total(stamps)}× gestempelt")
        }.joinToString(" · ")
    }
    NoteType.SKETCH -> "🖊️ Skizze"
    NoteType.TEXT -> body.trim()
}
