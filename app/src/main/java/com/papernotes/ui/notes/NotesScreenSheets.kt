package com.papernotes.ui.notes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.cardSurface
import com.papernotes.domain.model.earAccent
import com.papernotes.domain.toShareText
import com.papernotes.ui.components.ArchiveDrawerSheet
import com.papernotes.ui.components.CapsuleSheet
import com.papernotes.ui.components.ClipPickerSheet
import com.papernotes.ui.components.CountdownSheet
import com.papernotes.ui.components.ExpirySheet
import com.papernotes.ui.components.MoodOnlySheet
import com.papernotes.ui.components.MoodPickerSheet
import com.papernotes.ui.components.NoteLinkPickerSheet
import com.papernotes.ui.components.ReminderSheet
import com.papernotes.ui.components.SettingsSheet
import com.papernotes.ui.components.SortSheet
import com.papernotes.ui.components.TagPickerSheet
import com.papernotes.ui.theme.PaperTheme

/**
 * Zustand aller Bottom-Sheets des Grids. Wird von [NotesScreen] per `remember` gehalten;
 * Karten-Interaktionen setzen die Ziele, [NotesSheets] zeigt die passenden Sheets an.
 */
@Stable
internal class NotesSheetState {
    var moodTarget by mutableStateOf<Note?>(null)
    var tagTarget by mutableStateOf<Note?>(null)
    var reminderTarget by mutableStateOf<Note?>(null)
    var expiryTarget by mutableStateOf<Note?>(null)
    var countdownTarget by mutableStateOf<Note?>(null)
    var capsuleTarget by mutableStateOf<Note?>(null)
    var linkTarget by mutableStateOf<Note?>(null)
    var clipTarget by mutableStateOf<Note?>(null)
    var sortSheetOpen by mutableStateOf(false)
    var settingsSheetOpen by mutableStateOf(false)
    var selectionMoodOpen by mutableStateOf(false)
    var selectionTagOpen by mutableStateOf(false)
    var drawerOpen by mutableStateOf(false)
}

/**
 * Alle Bottom-Sheets des Grids (Stimmung, Reiter, Erinnerung, Ablauf, Countdown, Kapsel,
 * Faden, Klammer, Sortierung, Einstellungen, Auswahl-Aktionen, Schublade). Reine
 * Verschiebung aus [NotesScreen]; Querschnitts-Aktionen (Teilen, Knüddeln, Foto, Backup,
 * Anordnen) laufen über Callbacks, weil sie Zustand des Screens brauchen.
 */
@Composable
internal fun NotesSheets(
    state: NotesUiState,
    sheets: NotesSheetState,
    viewModel: NotesViewModel,
    sortMode: SortMode,
    currentTheme: PaperTheme,
    onPickTheme: (PaperTheme) -> Unit,
    onShare: (note: Note, surface: Color, ink: Color, accent: Color) -> Unit,
    onDelete: (note: Note, surface: Color) -> Unit,
    onAttachPhoto: (Note) -> Unit,
    onReminderSet: () -> Unit,
    onEnterArrange: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current

    // Stimmungs-/Pin-/Lösch-Sheet
    sheets.moodTarget?.let { target ->
        val targetSurface = target.mood.cardSurface()
        val targetAccent = target.mood.earAccent()
        val inkColor = MaterialTheme.colorScheme.onBackground
        MoodPickerSheet(
            selected = target.mood,
            pinned = target.pinned,
            hasReminder = target.hasReminder,
            sealed = target.sealed,
            invisibleInk = target.invisibleInk,
            done = target.done,
            hasExpiry = target.hasExpiry,
            hasCountdown = target.hasCountdown,
            hasCapsule = target.capsuleAt != null,
            hasPhoto = target.hasPhoto,
            paper = target.paper,
            onPick = { mood ->
                viewModel.setMood(target, mood)
                sheets.moodTarget = null
            },
            onPickPaper = { style ->
                viewModel.setPaper(target, style)
                sheets.moodTarget = target.copy(paper = style) // Auswahl sofort im Sheet spiegeln
            },
            onTogglePin = {
                viewModel.togglePin(target)
                sheets.moodTarget = null
            },
            onToggleSeal = {
                viewModel.toggleSeal(target)
                sheets.moodTarget = null
            },
            onSetCapsule = {
                sheets.moodTarget = null
                sheets.capsuleTarget = target
            },
            onToggleInvisibleInk = {
                viewModel.toggleInvisibleInk(target)
                sheets.moodTarget = null
            },
            onToggleDone = {
                viewModel.toggleDone(target)
                sheets.moodTarget = null
            },
            onEditTags = {
                sheets.moodTarget = null
                sheets.tagTarget = target
            },
            onSetExpiry = {
                sheets.expiryTarget = target
                sheets.moodTarget = null
            },
            onSetReminder = {
                sheets.moodTarget = null
                sheets.reminderTarget = target
            },
            onSetCountdown = {
                sheets.moodTarget = null
                sheets.countdownTarget = target
            },
            onAttachPhoto = {
                sheets.moodTarget = null
                onAttachPhoto(target)
            },
            onLink = {
                sheets.moodTarget = null
                sheets.linkTarget = target
            },
            onClip = {
                sheets.moodTarget = null
                sheets.clipTarget = target
            },
            onShare = {
                sheets.moodTarget = null
                onShare(target, targetSurface, inkColor, targetAccent)
            },
            onCopy = {
                clipboard.setText(AnnotatedString(target.toShareText()))
                sheets.moodTarget = null
            },
            onDelete = {
                sheets.moodTarget = null
                onDelete(target, targetSurface)
            },
            onDismiss = { sheets.moodTarget = null },
        )
    }

    // Karteireiter-Sheet (vom Grid-Long-Press)
    sheets.tagTarget?.let { target ->
        // Live-Notiz, damit die Chip-Auswahl die letzte Änderung sofort spiegelt.
        val live = state.notes.firstOrNull { it.note.id == target.id }?.note ?: target
        TagPickerSheet(
            noteTags = live.tagList,
            allTags = state.presentTags,
            onToggle = { tag -> viewModel.toggleTag(live, tag) },
            onAdd = { tag -> viewModel.addTag(live, tag) },
            onDismiss = { sheets.tagTarget = null },
        )
    }

    // Erinnerungs-Sheet (vom Grid-Long-Press)
    sheets.reminderTarget?.let { target ->
        ReminderSheet(
            currentReminderAt = target.reminderAt,
            currentRule = target.reminderRule,
            onPick = { at, rule ->
                viewModel.setReminder(target, at, rule)
                onReminderSet()
                sheets.reminderTarget = null
            },
            onClear = {
                viewModel.setReminder(target, null)
                sheets.reminderTarget = null
            },
            onDismiss = { sheets.reminderTarget = null },
        )
    }

    // Selbstzerstörung: Ablaufzeit der Notiz wählen/aufheben
    sheets.expiryTarget?.let { target ->
        ExpirySheet(
            currentExpiresAt = target.expiresAt,
            onPick = { at ->
                viewModel.setExpiry(target, at)
                sheets.expiryTarget = null
            },
            onClear = {
                viewModel.setExpiry(target, null)
                sheets.expiryTarget = null
            },
            onDismiss = { sheets.expiryTarget = null },
        )
    }

    // Abreißkalender: Zieldatum der Notiz wählen/abreißen
    sheets.countdownTarget?.let { target ->
        CountdownSheet(
            currentCountdownAt = target.countdownAt,
            onPick = { at ->
                viewModel.setCountdown(target, at)
                sheets.countdownTarget = null
            },
            onClear = {
                viewModel.setCountdown(target, null)
                sheets.countdownTarget = null
            },
            onDismiss = { sheets.countdownTarget = null },
        )
    }

    // Zeitkapsel: Notiz bis zu einem Datum versiegeln
    sheets.capsuleTarget?.let { target ->
        CapsuleSheet(
            currentCapsuleAt = target.capsuleAt,
            onPick = { at ->
                viewModel.setCapsule(target, at)
                sheets.capsuleTarget = null
            },
            onClear = {
                viewModel.setCapsule(target, null)
                sheets.capsuleTarget = null
            },
            onDismiss = { sheets.capsuleTarget = null },
        )
    }

    // Roter-Faden-Auswahl: andere Notiz zum Verknüpfen/Lösen wählen
    sheets.linkTarget?.let { target ->
        val linkedIds = state.links
            .filter { it.involves(target.id) }
            .mapNotNull { it.otherEnd(target.id) }
            .toSet()
        NoteLinkPickerSheet(
            candidates = state.notes.map { it.note }.filter { it.id != target.id },
            linkedIds = linkedIds,
            onToggle = { otherId ->
                if (otherId in linkedIds) {
                    viewModel.unlinkNotes(target.id, otherId)
                } else {
                    viewModel.linkNotes(target.id, otherId)
                }
            },
            onDismiss = { sheets.linkTarget = null },
        )
    }

    // Büroklammer-Auswahl: andere Notizen an den Stapel klammern/lösen
    sheets.clipTarget?.let { target ->
        val groupId = target.clipId ?: target.id
        val clippedIds = state.notes
            .map { it.note }
            .filter { it.id != target.id && it.clipId == groupId }
            .map { it.id }
            .toSet()
        ClipPickerSheet(
            candidates = state.notes.map { it.note }.filter { it.id != target.id },
            clippedIds = clippedIds,
            onToggle = { otherId -> viewModel.toggleClip(target, otherId) },
            onDismiss = { sheets.clipTarget = null },
        )
    }

    // Ordnen: Sortierung wählen oder frei anordnen
    if (sheets.sortSheetOpen) {
        SortSheet(
            selected = sortMode,
            onPick = { mode ->
                viewModel.setSortMode(mode)
                sheets.sortSheetOpen = false
            },
            onArrange = {
                // Freies Ziehen setzt die Sortierung auf Pinnwand zurück, sonst würde
                // die aktive Auto-Sortierung die gezogene Reihenfolge sofort überstimmen.
                viewModel.setSortMode(SortMode.PINNWAND)
                viewModel.clearSelection()
                onEnterArrange()
                sheets.sortSheetOpen = false
            },
            onDismiss = { sheets.sortSheetOpen = false },
        )
    }

    // Einstellungen-Zettel: Papier-Theme, Sicherung, Über
    if (sheets.settingsSheetOpen) {
        SettingsSheet(
            selectedTheme = currentTheme,
            onPickTheme = onPickTheme,
            onDismiss = { sheets.settingsSheetOpen = false },
            onExport = {
                sheets.settingsSheetOpen = false
                onExportBackup()
            },
            onImport = {
                sheets.settingsSheetOpen = false
                onImportBackup()
            },
        )
    }

    // Stimmung für die Mehrfachauswahl (abgespeckte Farbreihe)
    if (sheets.selectionMoodOpen) {
        MoodOnlySheet(
            onPick = { mood ->
                viewModel.moodSelected(mood)
                sheets.selectionMoodOpen = false
            },
            onDismiss = { sheets.selectionMoodOpen = false },
        )
    }

    // Karteireiter für die Mehrfachauswahl (fügt den Tag allen gewählten Zetteln hinzu)
    if (sheets.selectionTagOpen) {
        TagPickerSheet(
            noteTags = emptyList(),
            allTags = state.presentTags,
            onToggle = { tag ->
                viewModel.tagSelected(tag)
                sheets.selectionTagOpen = false
            },
            onAdd = { tag ->
                viewModel.tagSelected(tag)
                sheets.selectionTagOpen = false
            },
            onDismiss = { sheets.selectionTagOpen = false },
        )
    }

    // Archiv-Schublade
    if (sheets.drawerOpen) {
        ArchiveDrawerSheet(
            archived = state.archived,
            trashed = state.trashed,
            onRestoreArchived = viewModel::restore,
            onRestoreTrashed = viewModel::restore,
            onDismiss = { sheets.drawerOpen = false },
        )
    }
}
