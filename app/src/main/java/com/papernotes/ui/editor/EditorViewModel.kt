package com.papernotes.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papernotes.data.prefs.DelightPreferences
import com.papernotes.data.repository.NoteRepository
import com.papernotes.domain.ChecklistCodec
import com.papernotes.domain.ChecklistItem
import com.papernotes.domain.Highlight
import com.papernotes.domain.SketchStroke
import com.papernotes.domain.StampCodec
import com.papernotes.domain.StampMotif
import com.papernotes.domain.model.MoodCategory
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.NoteType
import com.papernotes.domain.model.PaperStyle
import com.papernotes.domain.model.ReminderRule
import java.time.LocalDate
import com.papernotes.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Checklisten-Zeile mit stabiler UI-Id (für Move-/Sink-Animationen beim Sortieren). */
data class EditableChecklistItem(
    val uiId: Long,
    val text: String,
    val checked: Boolean,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val delightPreferences: DelightPreferences,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _note = MutableStateFlow(Note())
    val note: StateFlow<Note> = _note.asStateFlow()

    private val _items = MutableStateFlow<List<EditableChecklistItem>>(emptyList())
    val items: StateFlow<List<EditableChecklistItem>> = _items.asStateFlow()

    /** Tinten-Striche der aktuellen Skizze (leer für andere Notiz-Typen). */
    private val _strokes = MutableStateFlow<List<SketchStroke>>(emptyList())
    val strokes: StateFlow<List<SketchStroke>> = _strokes.asStateFlow()

    /** Zähler; jede Erhöhung = ein Konfetti-Burst (letzter offener Eintrag abgehakt). */
    private val _celebration = MutableStateFlow(0)
    val celebration: StateFlow<Int> = _celebration.asStateFlow()

    /** Zeile, die nach dem Anlegen den Fokus bekommen soll. */
    private val _focusRequest = MutableStateFlow<Long?>(null)
    val focusRequest: StateFlow<Long?> = _focusRequest.asStateFlow()

    /** Id der aktuell geladenen Notiz (0 = noch ungespeichert) – Basis für die roten Fäden. */
    private val currentId = MutableStateFlow(0L)

    /** Per rotem Faden verknüpfte Notizen (für die Sprung-Chips). */
    val linkedNotes: StateFlow<List<Note>> = combine(
        currentId,
        repository.observeLinks(),
        repository.observeActiveNotes(),
    ) { id, links, notes ->
        if (id == 0L) {
            emptyList()
        } else {
            val partners = links.filter { it.involves(id) }.mapNotNull { it.otherEnd(id) }.toSet()
            notes.filter { it.id in partners }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Verknüpfbare Notizen (alle aktiven außer der aktuellen). */
    val candidateNotes: StateFlow<List<Note>> = combine(
        currentId,
        repository.observeActiveNotes(),
    ) { id, notes ->
        notes.filter { it.id != id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Alle bereits vergebenen Karteireiter (über alle aktiven Notizen), für die Chip-Auswahl. */
    val allTags: StateFlow<List<String>> =
        repository.observeActiveNotes()
            .map { notes -> notes.flatMap { it.tagList }.distinct().sorted() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var loadedSession = -1
    private var saveJob: Job? = null
    private var nextUiId = 1L

    // --- Undo/Redo (Text, Rückseite, Textmarker, Checkliste) ---
    // Snapshot-basiert: vor jeder Mutation wird der alte Zustand gesichert; schnelle
    // Tastenanschläge im selben Feld werden zu einem Schritt gruppiert. Skizzen behalten
    // ihr eigenes undoStroke(), Stempel sind per erneutem Tipp trivial reversibel.

    private data class EditorSnapshot(
        val title: String,
        val body: String,
        val backText: String,
        val highlights: List<Highlight>,
        val items: List<EditableChecklistItem>,
    )

    private enum class EditField { TITLE, BODY, BACK, HIGHLIGHT, CHECKLIST }

    private val undoStack = ArrayDeque<EditorSnapshot>()
    private val redoStack = ArrayDeque<EditorSnapshot>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()
    private var lastSnapshotAt = 0L
    private var lastEditedField: EditField? = null

    private fun currentSnapshot() = EditorSnapshot(
        title = _note.value.title,
        body = _note.value.body,
        backText = _note.value.backText,
        highlights = _note.value.highlightRanges,
        items = _items.value,
    )

    private fun recordSnapshot(field: EditField, group: Boolean = true) {
        val now = System.currentTimeMillis()
        if (group && field == lastEditedField && now - lastSnapshotAt <= 800 && undoStack.isNotEmpty()) {
            lastSnapshotAt = now
            return
        }
        undoStack.addLast(currentSnapshot())
        while (undoStack.size > 50) undoStack.removeFirst()
        redoStack.clear()
        lastEditedField = field
        lastSnapshotAt = now
        updateUndoState()
    }

    fun undo() {
        val snapshot = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(currentSnapshot())
        applySnapshot(snapshot)
    }

    fun redo() {
        val snapshot = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(currentSnapshot())
        applySnapshot(snapshot)
    }

    private fun applySnapshot(s: EditorSnapshot) {
        _note.update {
            it.copy(title = s.title, body = s.body, backText = s.backText)
                .withHighlights(s.highlights)
        }
        _items.value = s.items
        lastEditedField = null
        updateUndoState()
        scheduleSave()
    }

    private fun updateUndoState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun resetHistory() {
        undoStack.clear()
        redoStack.clear()
        lastEditedField = null
        lastSnapshotAt = 0L
        updateUndoState()
    }

    /**
     * Lädt eine bestehende Notiz (id > 0) oder startet eine leere. [session] kommt vom NavGraph
     * und steigt bei *jedem* Öffnen — so wird der (Activity-weit geteilte) ViewModel-Zustand bei
     * jedem Editor-Aufruf frisch zurückgesetzt, auch beim wiederholten Anlegen neuer Notizen.
     */
    fun load(
        id: Long,
        newType: NoteType,
        session: Int,
        initialTitle: String = "",
        initialBody: String = "",
    ) {
        if (session == loadedSession) return
        loadedSession = session

        // Frischer Start: alten Zustand vollständig zurücksetzen.
        saveJob?.cancel()
        saveJob = null
        nextUiId = 1L
        _celebration.value = 0
        _focusRequest.value = null
        _items.value = emptyList()
        _strokes.value = emptyList()
        resetHistory()
        currentId.value = if (id > 0L) id else 0L

        if (id <= 0L) {
            _note.value = Note(type = newType, title = initialTitle, body = initialBody)
            if (newType == NoteType.CHECKLIST) addItem()
            // Der Start-Zustand (inkl. erster Leerzeile) ist kein Undo-Schritt.
            resetHistory()
            // Eingeklebter Inhalt soll sofort gespeichert werden (nicht erst nach Eingabe).
            if (initialTitle.isNotBlank() || initialBody.isNotBlank()) scheduleSave()
            return
        }
        _note.value = Note(id = id)
        viewModelScope.launch {
            repository.getNote(id)?.let { note ->
                if (loadedSession != session) return@launch
                // Öffnen = Quittieren: fällige Erinnerung beenden (Flattern + Notification weg).
                // Wiederkehrende Erinnerung wird stattdessen auf den nächsten Termin vorgerückt.
                val nowMs = System.currentTimeMillis()
                if (note.isReminderDue(nowMs)) {
                    reminderScheduler.dismissNotification(id)
                    // Ein evtl. geschlummerter Zusatz-Alarm ist mit dem Öffnen quittiert.
                    reminderScheduler.cancelSnooze(id)
                    if (note.isRecurring) {
                        var next = note.reminderRule.next(note.reminderAt!!)
                        while (next <= nowMs) next = note.reminderRule.next(next)
                        repository.setReminder(id, next)
                        reminderScheduler.schedule(id, note.title, next, note.reminderRule)
                        _note.value = note.copy(reminderAt = next)
                    } else {
                        repository.setReminder(id, null)
                        _note.value = note.copy(reminderAt = null)
                    }
                    _items.value = note.checklist.map { it.toEditable() }
                    _strokes.value = note.sketch
                    return@let
                }
                _note.value = note
                _items.value = note.checklist.map { it.toEditable() }
                _strokes.value = note.sketch
            }
        }
    }

    fun onTitleChange(value: String) {
        recordSnapshot(EditField.TITLE)
        _note.update { it.copy(title = value) }
        scheduleSave()
    }

    fun onBodyChange(value: String) {
        recordSnapshot(EditField.BODY)
        _note.update { note ->
            // Textmarker-Markierungen beim Editieren mitverschieben.
            val shifted = shiftHighlights(note.body, value, note.highlightRanges)
            note.copy(body = value).withHighlights(shifted)
        }
        scheduleSave()
    }

    /** Markiert die Auswahl [start, end) farbig – oder entfernt sie, wenn sie bereits markiert ist. */
    fun applyHighlight(start: Int, end: Int, color: Int) {
        if (end <= start) return
        recordSnapshot(EditField.HIGHLIGHT, group = false)
        _note.update { note ->
            val ranges = note.highlightRanges
            val next = if (isCovered(ranges, start, end)) {
                subtract(ranges, start, end)
            } else {
                subtract(ranges, start, end) + Highlight(start, end, color)
            }
            note.withHighlights(next)
        }
        scheduleSave()
    }

    /** Text auf der Rückseite des Blatts (unabhängig vom Notiz-Typ der Vorderseite). */
    fun onBackChange(value: String) {
        recordSnapshot(EditField.BACK)
        _note.update { it.copy(backText = value) }
        scheduleSave()
    }

    fun setMood(mood: MoodCategory) {
        _note.update { it.copy(mood = mood, dogEarFolded = true) }
        scheduleSave()
    }

    fun toggleDogEar() {
        _note.update { it.copy(dogEarFolded = !it.dogEarFolded) }
        scheduleSave()
    }

    fun togglePin() {
        _note.update { it.copy(pinned = !it.pinned) }
        scheduleSave()
    }

    fun toggleSeal() {
        _note.update { it.copy(sealed = !it.sealed) }
        scheduleSave()
    }

    fun toggleInvisibleInk() {
        _note.update { it.copy(invisibleInk = !it.invisibleInk) }
        scheduleSave()
    }

    fun toggleDone() {
        _note.update { it.copy(done = !it.done) }
        scheduleSave()
    }

    /** Schaltet einen Karteireiter an der Notiz an oder aus. */
    fun toggleTag(tag: String) {
        _note.update { note ->
            val current = note.tagList
            val next = if (tag in current) current - tag else current + tag
            note.withTags(next)
        }
        scheduleSave()
    }

    /** Legt einen neuen Karteireiter an (bzw. lässt vorhandene unverändert). */
    fun addTag(tag: String) {
        _note.update { it.withTags(it.tagList + tag) }
        scheduleSave()
    }

    /** Setzt/entfernt die Ablaufzeit (vergängliche Notiz); rein persistiert, kein Alarm nötig. */
    fun setExpiry(at: Long?) {
        _note.update { it.copy(expiresAt = at) }
        scheduleSave()
    }

    /** Zeitkapsel: versiegelt die Notiz und plant das Selbst-Öffnen zu [at] (null = auflösen). */
    fun setCapsule(at: Long?) {
        saveJob?.cancel()
        _note.update { it.copy(capsuleAt = at, sealed = if (at != null) true else it.sealed) }
        viewModelScope.launch {
            val snapshot = _note.value
            val id = repository.save(snapshot)
            if (snapshot.id == 0L) _note.update { if (it.id == 0L) it.copy(id = id) else it }
            if (at != null) reminderScheduler.scheduleCapsule(id, at)
            else reminderScheduler.cancelCapsule(id)
        }
    }

    /** Setzt/entfernt das Abreißkalender-Zieldatum; rein persistiert, kein Alarm nötig. */
    fun setCountdown(at: Long?) {
        _note.update { it.copy(countdownAt = at) }
        scheduleSave()
    }

    /** Setzt/entfernt den Foto-Dateinamen (Bild liegt bereits auf Platte). */
    fun setPhoto(path: String?) {
        _note.update { it.copy(photoPath = path) }
        scheduleSave()
    }

    /** Wählt die Papier-Liniierung. */
    fun setPaper(style: PaperStyle) {
        _note.update { it.copy(paper = style) }
        scheduleSave()
    }

    /**
     * Setzt ([at] != null) oder entfernt ([at] == null) die Erinnerung. Speichert sofort,
     * damit eine frische Notiz eine id bekommt, und plant/entfernt dann den exakten Alarm.
     */
    fun setReminder(at: Long?, rule: ReminderRule = ReminderRule.NONE) {
        saveJob?.cancel()
        _note.update {
            it.copy(reminderAt = at, reminderRule = if (at != null) rule else ReminderRule.NONE)
        }
        viewModelScope.launch {
            val snapshot = _note.value
            val id = repository.save(snapshot)
            if (snapshot.id == 0L) _note.update { if (it.id == 0L) it.copy(id = id) else it }
            if (at != null) {
                reminderScheduler.schedule(id, snapshot.title, at, snapshot.reminderRule)
            } else {
                reminderScheduler.cancel(id)
                reminderScheduler.dismissNotification(id)
            }
        }
    }

    // --- Checkliste ---

    fun setItemText(uiId: Long, text: String) {
        recordSnapshot(EditField.CHECKLIST)
        _items.update { list -> list.map { if (it.uiId == uiId) it.copy(text = text) else it } }
        syncChecklistBody()
    }

    fun toggleItem(uiId: Long) {
        recordSnapshot(EditField.CHECKLIST, group = false)
        val before = _items.value
        val target = before.firstOrNull { it.uiId == uiId } ?: return
        val nowChecked = !target.checked

        // Erledigte sinken ans Ende des offenen Blocks bzw. ganz nach unten.
        val rest = before.filter { it.uiId != uiId }
        val updated = target.copy(checked = nowChecked)
        _items.value = if (nowChecked) {
            rest + updated
        } else {
            rest.filter { !it.checked } + updated + rest.filter { it.checked }
        }
        syncChecklistBody()

        if (nowChecked) {
            viewModelScope.launch { delightPreferences.incrementChecksToday() }
            if (_items.value.all { it.checked }) _celebration.update { it + 1 }
        }
    }

    /** Fügt eine leere Zeile hinter [afterUiId] (oder ans Ende des offenen Blocks) ein. */
    fun addItem(afterUiId: Long? = null) {
        recordSnapshot(EditField.CHECKLIST, group = false)
        val newItem = EditableChecklistItem(uiId = nextUiId++, text = "", checked = false)
        _items.update { list ->
            val index = list.indexOfFirst { it.uiId == afterUiId }
            if (index >= 0) {
                list.toMutableList().apply { add(index + 1, newItem) }
            } else {
                val firstChecked = list.indexOfFirst { it.checked }
                if (firstChecked < 0) {
                    list + newItem
                } else {
                    list.toMutableList().apply { add(firstChecked, newItem) }
                }
            }
        }
        _focusRequest.value = newItem.uiId
        syncChecklistBody()
    }

    fun removeItem(uiId: Long) {
        recordSnapshot(EditField.CHECKLIST, group = false)
        _items.update { list -> list.filter { it.uiId != uiId } }
        syncChecklistBody()
    }

    fun consumeFocusRequest() {
        _focusRequest.value = null
    }

    // --- Stempelkarte ---

    /**
     * Setzt/entfernt den Stempel für [day] (Epoch-Tag) – auch für vergangene Tage zum
     * Nachstempeln. Konfetti, wenn ein neuer Stempel die aktuelle Strähne auf eine 7er-Marke hebt.
     */
    fun toggleStamp(day: Long) {
        val days = _note.value.stamps.toMutableSet()
        val nowStamped = if (day in days) {
            days.remove(day); false
        } else {
            days.add(day); true
        }
        _note.update { it.withStamps(days) }
        scheduleSave()
        if (nowStamped) {
            val streak = StampCodec.streak(days, LocalDate.now().toEpochDay())
            if (streak > 0 && streak % 7L == 0L) _celebration.update { it + 1 }
        }
    }

    /** Wählt das Stempel-Motiv der Karte (bewahrt die bereits gestempelten Tage). */
    fun setStampMotif(motif: StampMotif) {
        _note.update { it.withStampMotif(motif) }
        scheduleSave()
    }

    // --- Skizze ---

    /** Hängt einen fertig gezeichneten Strich an und persistiert die Skizze im body. */
    fun addStroke(stroke: SketchStroke) {
        if (stroke.points.isEmpty()) return
        _strokes.update { it + stroke }
        syncSketch()
    }

    /** Entfernt die mit dem Radierer berührten Striche (Indizes der aktuellen Liste). */
    fun eraseStrokes(indices: Set<Int>) {
        if (indices.isEmpty()) return
        _strokes.update { list -> list.filterIndexed { i, _ -> i !in indices } }
        syncSketch()
    }

    fun undoStroke() {
        if (_strokes.value.isEmpty()) return
        _strokes.update { it.dropLast(1) }
        syncSketch()
    }

    fun clearSketch() {
        if (_strokes.value.isEmpty()) return
        _strokes.value = emptyList()
        syncSketch()
    }

    private fun syncSketch() {
        _note.update { it.withSketch(_strokes.value) }
        scheduleSave()
    }

    // --- Roter Faden ---

    /** Verknüpft die (ggf. erst zu speichernde) Notiz mit [otherId]. */
    fun linkNotes(otherId: Long) = viewModelScope.launch {
        val id = ensureSaved()
        if (id != otherId) repository.linkNotes(id, otherId)
    }

    fun unlinkNotes(otherId: Long) = viewModelScope.launch {
        val id = _note.value.id
        if (id != 0L) repository.unlinkNotes(id, otherId)
    }

    // --- Büroklammer-Stapel ---

    /** Klammert [otherId] an den Stapel dieser Notiz (bzw. löst sie wieder). */
    fun toggleClip(otherId: Long) = viewModelScope.launch {
        val id = ensureSaved()
        if (id == otherId) return@launch
        val groupId = _note.value.clipId ?: id
        if (_note.value.clipId == null) {
            repository.setClip(id, groupId)
            _note.update { if (it.clipId == null) it.copy(clipId = groupId) else it }
        }
        val other = repository.getNote(otherId) ?: return@launch
        if (other.clipId == groupId) {
            repository.setClip(otherId, null)
        } else {
            repository.setClip(otherId, groupId)
        }
    }

    /** Stellt sicher, dass die Notiz eine id hat (für Verknüpfungen), und gibt sie zurück. */
    private suspend fun ensureSaved(): Long {
        val snapshot = _note.value
        if (snapshot.id != 0L) return snapshot.id
        val id = repository.save(snapshot)
        _note.update { if (it.id == 0L) it.copy(id = id) else it }
        currentId.value = id
        return id
    }

    // --- Persistenz ---

    /** Sofort speichern – beim Verlassen des Editors. Notiz wird sofort gefangen (race-sicher). */
    fun flush() {
        saveJob?.cancel()
        val snapshot = _note.value
        viewModelScope.launch { persist(snapshot) }
    }

    fun moveToTrash(onDone: () -> Unit) {
        saveJob?.cancel()
        val id = _note.value.id
        viewModelScope.launch {
            if (id != 0L) repository.moveToTrash(id)
            onDone()
        }
    }

    private fun ChecklistItem.toEditable() =
        EditableChecklistItem(uiId = nextUiId++, text = text, checked = checked)

    private fun syncChecklistBody() {
        _note.update { note ->
            note.copy(
                body = ChecklistCodec.serialize(
                    _items.value
                        .filter { it.text.isNotBlank() }
                        .map { ChecklistItem(it.text, it.checked) },
                ),
            )
        }
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            persist(_note.value)
        }
    }

    private suspend fun persist(note: Note) {
        if (note.isBlank && note.id == 0L) return
        val id = repository.save(note)
        if (note.id == 0L) {
            // Nur die id nachtragen, wenn der Editor noch dieselbe (frisch angelegte) Notiz zeigt.
            _note.update { if (it.id == 0L) it.copy(id = id) else it }
            if (currentId.value == 0L) currentId.value = id
        }
    }
}

// --- Textmarker-Hilfsfunktionen (reine Listen-/Offset-Operationen) ---

/** Verschiebt Markierungen, wenn sich [old] zu [new] ändert (Präfix-/Suffix-Diff). */
private fun shiftHighlights(old: String, new: String, ranges: List<Highlight>): List<Highlight> {
    if (ranges.isEmpty() || old == new) return ranges
    val maxPrefix = minOf(old.length, new.length)
    var p = 0
    while (p < maxPrefix && old[p] == new[p]) p++
    var s = 0
    while (s < maxPrefix - p && old[old.length - 1 - s] == new[new.length - 1 - s]) s++
    val changeStart = p
    val changeEndOld = old.length - s
    val delta = new.length - old.length

    fun map(x: Int): Int = when {
        x <= changeStart -> x
        x >= changeEndOld -> x + delta
        else -> changeStart
    }
    return ranges.mapNotNull { h ->
        val a = map(h.start).coerceIn(0, new.length)
        val b = map(h.end).coerceIn(0, new.length)
        if (b > a) Highlight(a, b, h.color) else null
    }
}

/** true, wenn [a, b) lückenlos von Markierungen überdeckt ist. */
private fun isCovered(ranges: List<Highlight>, a: Int, b: Int): Boolean {
    var cursor = a
    for (h in ranges.sortedBy { it.start }) {
        if (h.start <= cursor && h.end > cursor) cursor = h.end
        if (cursor >= b) return true
    }
    return cursor >= b
}

/** Entfernt das Intervall [a, b) aus allen Markierungen (teilt sie ggf. auf). */
private fun subtract(ranges: List<Highlight>, a: Int, b: Int): List<Highlight> =
    ranges.flatMap { h ->
        when {
            h.end <= a || h.start >= b -> listOf(h)                       // keine Überlappung
            else -> buildList {
                if (h.start < a) add(Highlight(h.start, a, h.color))      // linker Rest
                if (h.end > b) add(Highlight(b, h.end, h.color))          // rechter Rest
            }
        }
    }
