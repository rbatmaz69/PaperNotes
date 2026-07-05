package com.papernotes.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Gemeinsame Maße und Winkel, damit Leisten, Sheets und Zettel überall gleich sitzen.
 *
 * Bewusst NICHT vereinheitlicht: die horizontalen Content-Paddings der Screens
 * (Grid 16dp, Agenda 20dp, Editor 24dp) – Pinnwand, Schreibtisch und Blatt sind
 * unterschiedliche Papier-Kontexte und dürfen unterschiedlich atmen.
 */
object PaperDimens {
    /** Mindestgröße für Touch-Ziele; das sichtbare Symbol darf kleiner bleiben. */
    val touchTarget = 48.dp

    /** Höhe der Kopfleiste – identisch in Übersicht und Editor. */
    val topBarHeight = 46.dp

    /** Horizontales Innen-Padding aller Bottom-Sheets. */
    val sheetHPadding = 24.dp

    /** Eckenradius der Werkzeug-/Aktionsleisten (SelectionBar u. Ä.). */
    val actionCorner = 14.dp

    /** Dezente Schieflage für Werkzeugleisten – kaum merklich, nur „handgelegt". */
    const val TILT_SUBTLE = -0.6f

    /** Verspielte Schieflage für Zettel-Snackbars – sichtbar schief, wie hingeworfen. */
    const val TILT_PLAYFUL = -1.5f
}
