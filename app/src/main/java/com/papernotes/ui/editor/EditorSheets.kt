package com.papernotes.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.papernotes.domain.model.Note
import com.papernotes.domain.toShareText
import com.papernotes.ui.components.CapsuleSheet
import com.papernotes.ui.components.ClipPickerSheet
import com.papernotes.ui.components.CountdownSheet
import com.papernotes.ui.components.ExpirySheet
import com.papernotes.ui.components.MoodPickerSheet
import com.papernotes.ui.components.NoteLinkPickerSheet
import com.papernotes.ui.components.ReminderSheet
import com.papernotes.ui.components.TagPickerSheet
import com.papernotes.util.rememberPaperHaptics

/**
 * Zustand aller Bottom-Sheets des Editors. Wird von [EditorScreen] per `remember` gehalten;
 * die Kopfzeile öffnet das Options-Sheet, das die übrigen Sheets ansteuert.
 */
@Stable
internal class EditorSheetState {
    var showMood by mutableStateOf(false)
    var showTags by mutableStateOf(false)
    var showReminder by mutableStateOf(false)
    var showExpiry by mutableStateOf(false)
    var showCapsule by mutableStateOf(false)
    var showLinkPicker by mutableStateOf(false)
    var showClip by mutableStateOf(false)
    var showCountdown by mutableStateOf(false)
}

/**
 * Alle Bottom-Sheets des Editors. Reine Verschiebung aus [EditorScreen]; Querschnitts-Aktionen
 * (Teilen, Foto, Löschen mit Navigation) laufen über Callbacks, weil sie Zustand des Screens
 * bzw. die Navigation brauchen.
 */
@Composable
internal fun EditorSheets(
    note: Note,
    sheets: EditorSheetState,
    viewModel: EditorViewModel,
    linkedNotes: List<Note>,
    candidateNotes: List<Note>,
    allTags: List<String>,
    onShare: () -> Unit,
    onAttachPhoto: () -> Unit,
    onDelete: () -> Unit,
    onReminderSet: () -> Unit,
) {
    val haptics = rememberPaperHaptics()
    val clipboard = LocalClipboardManager.current

    if (sheets.showMood) {
        MoodPickerSheet(
            selected = note.mood,
            pinned = note.pinned,
            hasReminder = note.hasReminder,
            sealed = note.sealed,
            invisibleInk = note.invisibleInk,
            done = note.done,
            hasExpiry = note.hasExpiry,
            hasCountdown = note.hasCountdown,
            hasCapsule = note.capsuleAt != null,
            hasPhoto = note.hasPhoto,
            paper = note.paper,
            onPick = {
                haptics.tick()
                viewModel.setMood(it)
                sheets.showMood = false
            },
            onPickPaper = {
                haptics.tick()
                viewModel.setPaper(it)
            },
            onTogglePin = {
                haptics.tap()
                viewModel.togglePin()
                sheets.showMood = false
            },
            onToggleSeal = {
                haptics.stamp()
                viewModel.toggleSeal()
                sheets.showMood = false
            },
            onSetCapsule = {
                sheets.showMood = false
                sheets.showCapsule = true
            },
            onToggleInvisibleInk = {
                haptics.stamp()
                viewModel.toggleInvisibleInk()
                sheets.showMood = false
            },
            onToggleDone = {
                haptics.stamp()
                viewModel.toggleDone()
                sheets.showMood = false
            },
            onEditTags = {
                sheets.showMood = false
                sheets.showTags = true
            },
            onSetExpiry = {
                sheets.showMood = false
                sheets.showExpiry = true
            },
            onSetReminder = {
                sheets.showMood = false
                sheets.showReminder = true
            },
            onSetCountdown = {
                sheets.showMood = false
                sheets.showCountdown = true
            },
            onAttachPhoto = {
                sheets.showMood = false
                onAttachPhoto()
            },
            onLink = {
                sheets.showMood = false
                sheets.showLinkPicker = true
            },
            onClip = {
                sheets.showMood = false
                sheets.showClip = true
            },
            onShare = {
                sheets.showMood = false
                onShare()
            },
            onCopy = {
                sheets.showMood = false
                clipboard.setText(AnnotatedString(note.toShareText()))
            },
            onDelete = {
                sheets.showMood = false
                haptics.crumple()
                onDelete()
            },
            onDismiss = { sheets.showMood = false },
        )
    }

    if (sheets.showReminder) {
        ReminderSheet(
            currentReminderAt = note.reminderAt,
            currentRule = note.reminderRule,
            onPick = { at, rule ->
                haptics.tick()
                viewModel.setReminder(at, rule)
                onReminderSet()
                sheets.showReminder = false
            },
            onClear = {
                haptics.tap()
                viewModel.setReminder(null)
                sheets.showReminder = false
            },
            onDismiss = { sheets.showReminder = false },
        )
    }

    if (sheets.showCapsule) {
        CapsuleSheet(
            currentCapsuleAt = note.capsuleAt,
            onPick = { at ->
                haptics.stamp()
                viewModel.setCapsule(at)
                sheets.showCapsule = false
            },
            onClear = {
                haptics.tap()
                viewModel.setCapsule(null)
                sheets.showCapsule = false
            },
            onDismiss = { sheets.showCapsule = false },
        )
    }

    if (sheets.showExpiry) {
        ExpirySheet(
            currentExpiresAt = note.expiresAt,
            onPick = { at ->
                haptics.tick()
                viewModel.setExpiry(at)
                sheets.showExpiry = false
            },
            onClear = {
                haptics.tap()
                viewModel.setExpiry(null)
                sheets.showExpiry = false
            },
            onDismiss = { sheets.showExpiry = false },
        )
    }

    if (sheets.showCountdown) {
        CountdownSheet(
            currentCountdownAt = note.countdownAt,
            onPick = { at ->
                haptics.tick()
                viewModel.setCountdown(at)
                sheets.showCountdown = false
            },
            onClear = {
                viewModel.setCountdown(null)
                sheets.showCountdown = false
            },
            onDismiss = { sheets.showCountdown = false },
        )
    }

    if (sheets.showLinkPicker) {
        val linkedIds = linkedNotes.map { it.id }.toSet()
        NoteLinkPickerSheet(
            candidates = candidateNotes,
            linkedIds = linkedIds,
            onToggle = { otherId ->
                haptics.tick()
                if (otherId in linkedIds) {
                    viewModel.unlinkNotes(otherId)
                } else {
                    viewModel.linkNotes(otherId)
                }
            },
            onDismiss = { sheets.showLinkPicker = false },
        )
    }

    if (sheets.showClip) {
        val groupId = note.clipId ?: note.id
        val clippedIds = candidateNotes.filter { it.clipId == groupId }.map { it.id }.toSet()
        ClipPickerSheet(
            candidates = candidateNotes,
            clippedIds = clippedIds,
            onToggle = { otherId ->
                haptics.tick()
                viewModel.toggleClip(otherId)
            },
            onDismiss = { sheets.showClip = false },
        )
    }

    if (sheets.showTags) {
        TagPickerSheet(
            noteTags = note.tagList,
            allTags = allTags,
            onToggle = { tag ->
                haptics.tick()
                viewModel.toggleTag(tag)
            },
            onAdd = { tag ->
                haptics.tick()
                viewModel.addTag(tag)
            },
            onDismiss = { sheets.showTags = false },
        )
    }
}
