package com.papernotes.ui.notes

/**
 * Einmalige Papier-Tipps in Prioritätsreihenfolge. [key] ist der Persistenz-Schlüssel
 * in den Settings-Prefs ("hints_seen") und muss deshalb stabil bleiben.
 */
enum class PaperHint(val key: String) {
    LONG_PRESS("longpress"),
    DOG_EAR("dogear"),
    PINCH_COLUMNS("pinch"),
    BACK_FLIP("backflip"),
    TEABAG("teabag"),
}

/**
 * Wählt den nächsten fälligen Tipp: frühestens ab dem 3. App-Start, danach im Abstand
 * von mindestens zwei Starts (Tipp Nr. i wird erst ab Start `3 + 2*i` fällig) – so
 * tröpfeln die Tipps herein, statt neue Nutzer zu überschütten. Bereits gesehene
 * (oder selbst entdeckte) Tipps werden übersprungen. Der Aufrufer zeigt pro App-Start
 * höchstens einen Tipp.
 */
fun nextHint(opens: Int, seen: Set<String>): PaperHint? =
    PaperHint.entries.firstOrNull { hint ->
        hint.key !in seen && opens >= 3 + 2 * hint.ordinal
    }
