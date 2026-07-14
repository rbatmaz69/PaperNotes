package com.papernotes.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.NoteType
import com.papernotes.domain.model.earAccent
import com.papernotes.ui.components.INK_PALETTE
import com.papernotes.ui.components.PEN_MEDIUM
import com.papernotes.ui.components.SketchCanvas
import com.papernotes.ui.components.SketchToolbar
import com.papernotes.ui.components.StampCard
import com.papernotes.ui.components.StampMotifPicker
import com.papernotes.ui.components.highlightTransformation
import com.papernotes.ui.components.paperRuling
import com.papernotes.util.rememberPaperHaptics
import java.time.LocalDate

/**
 * Body-Feld des Text-Editors als Screen-Zustand: [bodyValue] hält Text + Auswahl,
 * [lastSelection] merkt sich die letzte echte Auswahl, damit das Antippen einer
 * Textmarker-Farbe in der Kopfzeile den Fokus verlieren darf, ohne sie zu vergessen.
 */
@Stable
internal class EditorBodyState(initialText: String) {
    var bodyValue by mutableStateOf(TextFieldValue(initialText))
    var lastSelection by mutableStateOf(TextRange.Zero)
}

/**
 * Vorderseite des Blatts, abhängig vom Notiz-Typ: Checkliste, Stempelkarte, Skizze oder
 * Fließtext. Reine Verschiebung aus [EditorScreen].
 */
@Composable
internal fun EditorNoteContent(
    note: Note,
    viewModel: EditorViewModel,
    items: List<EditableChecklistItem>,
    strokes: List<com.papernotes.domain.SketchStroke>,
    focusRequestId: Long?,
    bodyState: EditorBodyState,
    bodyFocus: FocusRequester,
    ink: Color,
) {
    val haptics = rememberPaperHaptics()
    when (note.type) {
        NoteType.CHECKLIST -> ChecklistEditor(
            items = items,
            focusRequestId = focusRequestId,
            accentColor = note.mood.earAccent(),
            onConsumeFocusRequest = viewModel::consumeFocusRequest,
            onToggle = viewModel::toggleItem,
            onTextChange = viewModel::setItemText,
            onAddAfter = viewModel::addItem,
            onRemove = viewModel::removeItem,
            modifier = Modifier
                .fillMaxSize()
                .paperRuling(note.paper, ink)
                .padding(horizontal = 24.dp),
            header = {
                TitleField(
                    title = note.title,
                    onTitleChange = viewModel::onTitleChange,
                    onNext = {},
                )
            },
        )

        NoteType.STAMPCARD -> Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            TitleField(
                title = note.title,
                onTitleChange = viewModel::onTitleChange,
                onNext = {},
            )
            StampMotifPicker(
                selected = note.stampMotif,
                accent = note.mood.earAccent(),
                onPick = {
                    haptics.tick()
                    viewModel.setStampMotif(it)
                },
                modifier = Modifier.padding(top = 8.dp),
            )
            StampCard(
                stamps = note.stamps,
                motif = note.stampMotif,
                today = LocalDate.now().toEpochDay(),
                accent = note.mood.earAccent(),
                onToggleDay = viewModel::toggleStamp,
                compact = false,
                modifier = Modifier.padding(top = 16.dp, bottom = 48.dp),
            )
        }

        NoteType.SKETCH -> {
            var penColor by remember { mutableStateOf(INK_PALETTE[0]) }
            var penWidth by remember { mutableFloatStateOf(PEN_MEDIUM) }
            var eraseMode by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
            ) {
                TitleField(
                    title = note.title,
                    onTitleChange = viewModel::onTitleChange,
                    onNext = {},
                )
                SketchToolbar(
                    penColor = penColor,
                    penWidth = penWidth,
                    eraseMode = eraseMode,
                    onPenColor = { penColor = it; eraseMode = false },
                    onPenWidth = { penWidth = it; eraseMode = false },
                    onToggleErase = {
                        haptics.tick()
                        eraseMode = !eraseMode
                    },
                    onUndo = {
                        haptics.tick()
                        viewModel.undoStroke()
                    },
                    onClear = {
                        haptics.tick()
                        viewModel.clearSketch()
                    },
                    modifier = Modifier.padding(top = 4.dp),
                )
                SketchCanvas(
                    strokes = strokes,
                    penColor = penColor,
                    penWidthDp = penWidth,
                    eraseMode = eraseMode,
                    defaultColor = note.mood.earAccent(),
                    onStrokeFinished = {
                        haptics.tick()
                        viewModel.addStroke(it)
                    },
                    onErase = viewModel::eraseStrokes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 8.dp, bottom = 24.dp),
                )
            }
        }

        NoteType.TEXT -> Column(
            modifier = Modifier
                .fillMaxSize()
                .paperRuling(note.paper, ink)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            TitleField(
                title = note.title,
                onTitleChange = viewModel::onTitleChange,
                onNext = { bodyFocus.requestFocus() },
            )

            BasicTextField(
                value = bodyState.bodyValue,
                onValueChange = { v ->
                    viewModel.onBodyChange(v.text)
                    // Letzte echte Auswahl merken – das Antippen einer Marker-Farbe
                    // oben darf den Text-Fokus verlieren, ohne die Auswahl zu vergessen.
                    if (!v.selection.collapsed) bodyState.lastSelection = v.selection
                    bodyState.bodyValue = v
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                cursorBrush = SolidColor(ink),
                visualTransformation = highlightTransformation(note.highlightRanges),
                decorationBox = { inner ->
                    if (note.body.isEmpty()) {
                        Text(
                            text = "Schreib los …",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    inner()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(bodyFocus)
                    .padding(bottom = 48.dp),
            )
        }
    }
}
