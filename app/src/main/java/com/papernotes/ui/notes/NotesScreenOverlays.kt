package com.papernotes.ui.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.papernotes.ui.components.ConfettiBurst
import com.papernotes.ui.components.CrumpleOverlay
import com.papernotes.ui.components.CrumpleRequest
import com.papernotes.ui.components.PaperPlaneOverlay
import com.papernotes.ui.components.PaperPlaneRequest
import com.papernotes.ui.components.WaxSealBreakOverlay
import com.papernotes.ui.components.WaxSealBreakRequest
import com.papernotes.util.shareImage
import com.papernotes.util.sharePlainText

/**
 * Zustand der Vollbild-Overlays des Grids (Knüddeln, Papierflieger, Siegelbruch, Konfetti).
 * Wird von [NotesScreen] per `remember` gehalten; die Karten-Interaktionen setzen die
 * Requests, [NotesOverlays] spielt sie ab.
 */
@Stable
internal class NotesOverlayState {
    var crumple by mutableStateOf<CrumpleRequest?>(null)
    var sealBreak by mutableStateOf<WaxSealBreakRequest?>(null)
    var shareRequest by mutableStateOf<PaperPlaneRequest?>(null)
    var shareText by mutableStateOf("")
    var shareUri by mutableStateOf<android.net.Uri?>(null)
    var stampConfettiKey by mutableIntStateOf(0)
    var showStampConfetti by mutableStateOf(false)
}

@Composable
internal fun NotesOverlays(
    overlays: NotesOverlayState,
    onTrash: (Long) -> Unit,
    onOpenNote: (Long) -> Unit,
) {
    val context = LocalContext.current

    // Knüddel-Animation → Papierkorb
    overlays.crumple?.let { req ->
        CrumpleOverlay(
            request = req,
            onFinished = {
                onTrash(req.noteId)
                overlays.crumple = null
            },
        )
    }

    // Papierflieger-Animation → Android-Teilen-Auswahl
    overlays.shareRequest?.let { req ->
        PaperPlaneOverlay(
            request = req,
            onFinished = {
                val uri = overlays.shareUri
                if (uri != null) {
                    context.shareImage(uri, overlays.shareText)
                } else {
                    context.sharePlainText(overlays.shareText)
                }
                overlays.shareRequest = null
                overlays.shareUri = null
            },
        )
    }

    // Wachssiegel zerspringt → öffnet die versiegelte Notiz
    overlays.sealBreak?.let { req ->
        WaxSealBreakOverlay(
            request = req,
            onFinished = {
                onOpenNote(req.noteId)
                overlays.sealBreak = null
            },
        )
    }

    // Stempel-Meilenstein: Konfetti über dem Grid
    if (overlays.showStampConfetti) {
        ConfettiBurst(
            trigger = overlays.stampConfettiKey,
            onFinished = { overlays.showStampConfetti = false },
        )
    }
}
