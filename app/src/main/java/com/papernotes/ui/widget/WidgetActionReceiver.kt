package com.papernotes.ui.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.papernotes.data.prefs.DelightPreferences
import com.papernotes.data.repository.NoteRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Empfängt Taps auf Checklisten-Zeilen des Haftnotiz-Widgets und toggelt das Item direkt
 * in Room. Bewusst in-place (kein Absinken wie im Editor): die Zeile bleibt unterm Finger,
 * sonst träfe ein zweiter Tap das falsche Item.
 */
@AndroidEntryPoint
class WidgetActionReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: NoteRepository

    @Inject lateinit var delightPreferences: DelightPreferences

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE_ITEM) return
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, 0L)
        val index = intent.getIntExtra(EXTRA_ITEM_INDEX, -1)
        if (noteId == 0L || index < 0) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val note = repository.getNote(noteId) ?: return@launch
                val items = note.checklist
                // Bounds-Check: die Liste kann parallel in der App geändert worden sein.
                if (index !in items.indices) return@launch
                val target = items[index]
                val next = items.toMutableList()
                    .also { it[index] = target.copy(checked = !target.checked) }
                repository.save(note.withChecklist(next))
                // Haken zählen für die Tagesstatistik (konsistent zum Editor).
                if (!target.checked) delightPreferences.incrementChecksToday()
            } finally {
                // Direkt neu rendern (noch innerhalb des goAsync-Fensters, kein Fire-and-forget).
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, StickyNoteWidgetReceiver::class.java),
                )
                ids.forEach { runCatching { StickyNoteWidgets.render(context, manager, it) } }
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_ITEM = "com.papernotes.widget.ACTION_TOGGLE_ITEM"
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_ITEM_INDEX = "item_index"
    }
}
