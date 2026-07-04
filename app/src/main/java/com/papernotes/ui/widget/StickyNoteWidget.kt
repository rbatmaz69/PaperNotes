package com.papernotes.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.papernotes.MainActivity
import com.papernotes.PaperNotesApp
import com.papernotes.R
import com.papernotes.domain.model.MoodCategory
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.NoteType
import com.papernotes.domain.toShareBody
import com.papernotes.reminder.ReminderReceiver
import com.papernotes.ui.theme.Espresso
import com.papernotes.ui.theme.InkCream
import com.papernotes.ui.theme.PaperTheme
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

/** Sentinel-Wert: das Widget folgt der Stimmungsfarbe der Notiz (hell/dunkel nach System). */
const val PALETTE_MOOD = "MOOD"

/** Liefert (Papierfarbe, Tintenfarbe) für die gewählte Palette. */
internal fun resolveColors(palette: String, mood: MoodCategory, night: Boolean): Pair<Color, Color> =
    if (palette == PALETTE_MOOD) {
        (if (night) mood.surfaceNight else mood.surface) to (if (night) InkCream else Espresso)
    } else {
        val theme = PaperTheme.fromKey(palette)
        theme.paper to theme.ink
    }

/**
 * „Haftnotiz"-Widget als klassische [RemoteViews] (kein Glance): zeigt genau eine Notiz als
 * Papierzettel. Tippen öffnet sie im Editor; ein noch leeres Widget öffnet die Auswahl. Updates
 * laufen direkt über [AppWidgetManager.updateAppWidget] – sofort und zuverlässig.
 */
object StickyNoteWidgets {

    /** Rendert eine Instanz: Konfig (SharedPreferences) + Notiz (DB) → RemoteViews. */
    suspend fun render(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
        val noteId = WidgetPrefs.noteId(context, appWidgetId)
        val palette = WidgetPrefs.palette(context, appWidgetId)
        val repo = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .noteRepository()
        // Im Papierkorb liegende Notiz nicht anzeigen (sonst „Geister"-Zettel).
        val note = if (noteId != 0L) repo.getNote(noteId)?.takeIf { it.deletedAt == null } else null

        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val mood = note?.mood ?: MoodCategory.PLAIN
        val (paperColor, inkColor) = resolveColors(palette, mood, night)
        val ink = inkColor.toArgb()

        val views = RemoteViews(context.packageName, R.layout.sticky_note_widget)
        views.setColorStateList(
            R.id.widget_root, "setBackgroundTintList", ColorStateList.valueOf(paperColor.toArgb()),
        )

        if (note == null) {
            views.setViewVisibility(R.id.widget_title, View.GONE)
            views.setViewVisibility(R.id.widget_body, View.VISIBLE)
            views.setTextViewText(R.id.widget_body, "Tippe, um einen Zettel zu wählen")
            views.setTextColor(R.id.widget_body, ink)
        } else {
            if (note.title.isBlank()) {
                views.setViewVisibility(R.id.widget_title, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_title, View.VISIBLE)
                views.setTextViewText(R.id.widget_title, note.title)
                views.setTextColor(R.id.widget_title, ink)
            }
            if (note.type == NoteType.CHECKLIST && note.checklist.isNotEmpty()) {
                // Checkliste: antippbare ☐/☑-Zeilen statt Fließtext-Zusammenfassung.
                views.setViewVisibility(R.id.widget_body, View.GONE)
                renderChecklist(context, views, note, appWidgetId, inkColor)
            } else {
                val body = note.toShareBody()
                if (body.isBlank()) {
                    views.setViewVisibility(R.id.widget_body, View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_body, View.VISIBLE)
                    views.setTextViewText(R.id.widget_body, body)
                    views.setTextColor(R.id.widget_body, ink)
                }
            }
        }

        // Konfiguriert → Notiz öffnen, sonst Auswahl öffnen. Eindeutige Data-URI pro Ziel.
        val clickIntent = if (note != null) {
            Intent(context, MainActivity::class.java).apply {
                data = Uri.parse("papernotes://note/${note.id}")
                putExtra(ReminderReceiver.EXTRA_NOTE_ID, note.id)
            }
        } else {
            Intent(context, StickyNoteConfigActivity::class.java).apply {
                data = Uri.parse("papernotes://configure/$appWidgetId")
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        manager.updateAppWidget(appWidgetId, views)
        android.util.Log.d(
            "StickyWidget",
            "rendered id=$appWidgetId noteId=$noteId palette=$palette note=${note?.id}",
        )
    }

    private val ITEM_IDS = intArrayOf(
        R.id.widget_item_0, R.id.widget_item_1, R.id.widget_item_2,
        R.id.widget_item_3, R.id.widget_item_4, R.id.widget_item_5,
    )

    /**
     * Bis zu [ITEM_IDS.size] feste Checklisten-Zeilen; Überhang wird als „+N weitere"
     * angedeutet. Pro Zeile ein Broadcast an den [WidgetActionReceiver] – die eindeutige
     * Data-URI hält die PendingIntents derselben Instanz auseinander.
     */
    private fun renderChecklist(
        context: Context,
        views: RemoteViews,
        note: Note,
        appWidgetId: Int,
        inkColor: Color,
    ) {
        val items = note.checklist
        val ink = inkColor.toArgb()
        val dimmed = inkColor.copy(alpha = 0.55f).toArgb()
        ITEM_IDS.forEachIndexed { index, viewId ->
            if (index >= items.size) return@forEachIndexed
            val item = items[index]
            views.setViewVisibility(viewId, View.VISIBLE)
            views.setTextViewText(viewId, (if (item.checked) "☑  " else "☐  ") + item.text)
            views.setTextColor(viewId, if (item.checked) dimmed else ink)
            val toggleIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = WidgetActionReceiver.ACTION_TOGGLE_ITEM
                data = Uri.parse("papernotes://widget/$appWidgetId/toggle/$index")
                putExtra(WidgetActionReceiver.EXTRA_NOTE_ID, note.id)
                putExtra(WidgetActionReceiver.EXTRA_ITEM_INDEX, index)
            }
            views.setOnClickPendingIntent(
                viewId,
                PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    toggleIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        }
        if (items.size > ITEM_IDS.size) {
            views.setViewVisibility(R.id.widget_more, View.VISIBLE)
            views.setTextViewText(R.id.widget_more, "+${items.size - ITEM_IDS.size} weitere")
            views.setTextColor(R.id.widget_more, dimmed)
        }
    }

    /** Eine Instanz neu rendern (z. B. nach der Konfiguration). */
    fun update(context: Context, appWidgetId: Int) {
        val manager = AppWidgetManager.getInstance(context)
        appScope(context).launch { runCatching { render(context, manager, appWidgetId) } }
    }

    /** Alle Haftnotiz-Widgets neu rendern (z. B. nach Notiz-Bearbeitungen). */
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, StickyNoteWidgetReceiver::class.java),
        )
        appScope(context).launch {
            ids.forEach { runCatching { render(context, manager, it) } }
        }
    }

    private fun appScope(context: Context) =
        (context.applicationContext as PaperNotesApp).appScope
}
