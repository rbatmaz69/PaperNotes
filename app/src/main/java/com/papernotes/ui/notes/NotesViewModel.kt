package com.papernotes.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papernotes.data.delight.DailyDelightProvider
import com.papernotes.data.prefs.DelightPreferences
import com.papernotes.data.prefs.SettingsPreferences
import com.papernotes.data.repository.NoteRepository
import com.papernotes.domain.StampCodec
import com.papernotes.domain.model.DailyDelight
import com.papernotes.domain.model.MoodCategory
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.NoteLink
import com.papernotes.domain.model.PaperStyle
import com.papernotes.domain.model.ReminderRule
import com.papernotes.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Collator
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * Sortierung des Grids. PINNWAND = die frei gezogene Reihenfolge (position-Spalte),
 * die anderen sortieren automatisch; Washi-Tape-Zettel bleiben in allen Modi oben.
 */
enum class SortMode { PINNWAND, CREATED, TITLE }

/** Rückgängig machbare Grid-Aktion für den Papierstreifen-Snackbar. */
enum class UndoKind { TRASH, ARCHIVE }

/** [id] ist ein laufender Zähler, damit ein neues Event den Auto-Dismiss-Timer neu startet. */
data class UndoEvent(
    val id: Long,
    val message: String,
    val actionLabel: String,
    val kind: UndoKind,
    val noteIds: List<Long>,
)

/** Notiz fürs Grid; [dimmed] = Nicht-Treffer von Suche/Stimmungs-Filter ("verdünnte Tinte"). */
data class GridNote(
    val note: Note,
    val dimmed: Boolean,
)

/**
 * Eine Grid-Zelle: entweder eine einzelne Notiz ([SoloItem]) oder ein Büroklammer-Stapel
 * ([StackItem]) aus mehreren zusammengeklammerten Notizen.
 */
sealed interface GridItem {
    val key: String
    val dimmed: Boolean
}

data class SoloItem(val gridNote: GridNote) : GridItem {
    override val key: String get() = "note-${gridNote.note.id}"
    override val dimmed: Boolean get() = gridNote.dimmed
}

/** Stapel: [cover] liegt oben, [members] enthält alle (inkl. Cover) in Sortier-Reihenfolge. */
data class StackItem(
    val clipId: Long,
    val cover: GridNote,
    val members: List<GridNote>,
) : GridItem {
    override val key: String get() = "clip-$clipId"
    override val dimmed: Boolean get() = members.all { it.dimmed }
}

data class NotesUiState(
    val notes: List<GridNote> = emptyList(),
    val items: List<GridItem> = emptyList(),
    val archived: List<Note> = emptyList(),
    val trashed: List<Note> = emptyList(),
    val presentMoods: List<MoodCategory> = emptyList(),
    val activeMoodFilter: MoodCategory? = null,
    val presentTags: List<String> = emptyList(),
    val activeTagFilter: String? = null,
    val searchQuery: String = "",
    val links: List<NoteLink> = emptyList(),
    val delight: DailyDelight,
    /** false, solange Room noch nicht das erste Mal emittiert hat (für Splash & Leerzustand). */
    val loaded: Boolean = false,
    val delightAvailable: Boolean = true,
    val statsLine: String = "",
)

/** Zwischenergebnis: Grid-Notizen + (zu Stapeln gruppierte) Grid-Items + Filter-Metadaten + Fäden. */
private data class GridState(
    val notes: List<GridNote>,
    val items: List<GridItem>,
    val presentMoods: List<MoodCategory>,
    val activeMood: MoodCategory?,
    val presentTags: List<String>,
    val activeTag: String?,
    val links: List<NoteLink>,
)

/**
 * Fasst die (bereits sortierten) Grid-Notizen zu Grid-Items zusammen: Notizen mit gleichem
 * `clipId` werden – sofern ≥2 aktiv sichtbar – an der Position der obersten zu einem [StackItem]
 * gebündelt; alle anderen bleiben [SoloItem]. Die ursprüngliche Reihenfolge bleibt erhalten.
 */
private fun groupIntoItems(notes: List<GridNote>): List<GridItem> {
    val byClip = notes.filter { it.note.clipId != null }.groupBy { it.note.clipId!! }
    val emitted = mutableSetOf<Long>()
    val items = mutableListOf<GridItem>()
    for (gridNote in notes) {
        val clipId = gridNote.note.clipId
        val group = clipId?.let { byClip[it] }
        if (clipId != null && group != null && group.size >= 2) {
            if (emitted.add(clipId)) {
                items += StackItem(clipId = clipId, cover = group.first(), members = group)
            }
        } else {
            items += SoloItem(gridNote)
        }
    }
    return items
}

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val delightPreferences: DelightPreferences,
    private val settingsPreferences: SettingsPreferences,
    private val reminderScheduler: ReminderScheduler,
    delightProvider: DailyDelightProvider,
) : ViewModel() {

    private val delight = delightProvider.forToday()

    private val searchQuery = MutableStateFlow("")
    private val moodFilter = MutableStateFlow<MoodCategory?>(null)
    private val tagFilter = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { repository.purgeOldTrash() }
        // Abgelaufene Notizen (während die App zu war) beim Start in den Papierkorb räumen.
        viewModelScope.launch { repository.purgeExpired() }
        // Auswahl gegen die sichtbaren Notizen schneiden (z. B. nach Ablauf/Extern-Änderung).
        viewModelScope.launch {
            repository.observeActiveNotes().collect { notes ->
                val visible = notes.mapTo(mutableSetOf()) { it.id }
                _selectedIds.update { it intersect visible }
            }
        }
        // Einmaliger Langdruck-Hinweis: erst ab dem 3. Start, nur solange er nie gezeigt
        // wurde und es überhaupt mehrere Zettel zum Auswählen gibt.
        viewModelScope.launch {
            val opens = settingsPreferences.incrementAppOpens()
            if (opens >= 3 && !settingsPreferences.longPressHintSeen.first()) {
                if (repository.observeActiveNotes().first().size >= 2) {
                    _longPressHint.value = true
                }
            }
        }
    }

    // --- Mehrfachauswahl ---

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    private val _longPressHint = MutableStateFlow(false)
    /** true, wenn der einmalige „Halte einen Zettel gedrückt"-Tipp gezeigt werden soll. */
    val longPressHint: StateFlow<Boolean> = _longPressHint

    private var longPressUsed = false

    /** Blendet den Tipp aus und merkt sich dauerhaft, dass er erledigt ist. */
    fun dismissLongPressHint() {
        _longPressHint.value = false
        viewModelScope.launch { settingsPreferences.markLongPressHintSeen() }
    }

    /** Toggelt die Auswahl; Stapel werden als Einheit gewählt/abgewählt. */
    fun toggleSelected(item: GridItem) {
        // Auswahl selbst entdeckt → Tipp ist überflüssig, stumm als gesehen markieren.
        if (!longPressUsed) {
            longPressUsed = true
            if (_longPressHint.value) _longPressHint.value = false
            viewModelScope.launch { settingsPreferences.markLongPressHintSeen() }
        }
        val ids = when (item) {
            is SoloItem -> setOf(item.gridNote.note.id)
            is StackItem -> item.members.mapTo(mutableSetOf()) { it.note.id }
        }
        _selectedIds.update { current -> if (ids.all { it in current }) current - ids else current + ids }
    }

    fun clearSelection() = _selectedIds.update { emptySet() }

    fun archiveSelected() = viewModelScope.launch {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return@launch
        clearSelection()
        repository.archiveMany(ids)
        pushUndo(UndoKind.ARCHIVE, ids)
    }

    fun trashSelected() = viewModelScope.launch {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return@launch
        clearSelection()
        repository.moveToTrashMany(ids)
        pushUndo(UndoKind.TRASH, ids)
    }

    /** Mixed-State-Heuristik: ist irgendein gewählter Zettel ungepinnt, werden alle gepinnt. */
    fun pinSelected() = viewModelScope.launch {
        val selected = _selectedIds.value
        if (selected.isEmpty()) return@launch
        val notes = uiState.value.notes.map { it.note }.filter { it.id in selected }
        repository.setPinnedMany(selected.toList(), pinned = notes.any { !it.pinned })
    }

    fun moodSelected(mood: MoodCategory) = viewModelScope.launch {
        val ids = _selectedIds.value.toList()
        if (ids.isNotEmpty()) repository.setMoodMany(ids, mood)
    }

    fun tagSelected(tag: String) = viewModelScope.launch {
        val ids = _selectedIds.value.toList()
        if (ids.isNotEmpty()) repository.addTagMany(ids, tag)
    }

    /** Persistierter Sortier-Modus (DataStore); unbekannte Werte fallen auf PINNWAND zurück. */
    val sortMode: StateFlow<SortMode> = settingsPreferences.sortModeKey
        .map { key -> runCatching { SortMode.valueOf(key) }.getOrDefault(SortMode.PINNWAND) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SortMode.PINNWAND)

    fun setSortMode(mode: SortMode) = viewModelScope.launch {
        settingsPreferences.setSortModeKey(mode.name)
    }

    /** Persistierte Spaltenzahl des Grids (Pinch-Zoom, 1–3). */
    val gridColumns: StateFlow<Int> = settingsPreferences.gridColumns
        .stateIn(viewModelScope, SharingStarted.Eagerly, 2)

    fun setGridColumns(columns: Int) = viewModelScope.launch {
        settingsPreferences.setGridColumns(columns)
    }

    // Mood + Tag vorab bündeln: combine() ist unten sonst am 5-Flow-Limit.
    private val filterState = combine(moodFilter, tagFilter) { mood, tag -> mood to tag }

    private val gridState = combine(
        repository.observeActiveNotes(),
        searchQuery,
        filterState,
        sortMode,
        repository.observeLinks(),
    ) { unsorted, query, (mood, tag), sort, links ->
        val notes = sortNotes(unsorted, sort)
        val trimmed = query.trim()
        val gridNotes = notes.map { note ->
            // Versiegelte Notizen tauchen nicht in Suchtreffern auf (kein Inhalts-Leak),
            // bleiben aber ohne aktive Suche normal im Grid sichtbar.
            val matchesQuery = trimmed.isEmpty() ||
                (!note.sealed && (
                    note.title.contains(trimmed, ignoreCase = true) ||
                        note.body.contains(trimmed, ignoreCase = true) ||
                        note.backText.contains(trimmed, ignoreCase = true) ||
                        note.tagList.any { it.contains(trimmed, ignoreCase = true) }
                    ))
            val matchesMood = mood == null || note.mood == mood
            val matchesTag = tag == null || tag in note.tagList
            GridNote(note = note, dimmed = !(matchesQuery && matchesMood && matchesTag))
        }
        GridState(
            notes = gridNotes,
            items = groupIntoItems(gridNotes),
            presentMoods = notes.map { it.mood }.distinct().sortedBy { it.ordinal },
            activeMood = mood,
            presentTags = notes.flatMap { it.tagList }.distinct().sorted(),
            activeTag = tag,
            links = links,
        )
    }

    private val dailyStats = combine(
        repository.countCreatedSince(startOfToday()),
        delightPreferences.checksToday,
    ) { created, checks ->
        buildString {
            append("Heute · ")
            append(if (created == 1) "1 Notiz" else "$created Notizen")
            if (checks > 0) append(" · $checks Haken")
        }
    }

    val uiState: StateFlow<NotesUiState> =
        combine(
            gridState,
            repository.observeArchivedNotes(),
            repository.observeTrashedNotes(),
            delightPreferences.availableToday,
            dailyStats,
        ) { grid, archived, trashed, available, stats ->
            NotesUiState(
                notes = grid.notes,
                items = grid.items,
                archived = archived,
                trashed = trashed,
                presentMoods = grid.presentMoods,
                activeMoodFilter = grid.activeMood,
                presentTags = grid.presentTags,
                activeTagFilter = grid.activeTag,
                searchQuery = searchQuery.value,
                links = grid.links,
                delight = delight,
                loaded = true,
                delightAvailable = available,
                statsLine = stats,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = NotesUiState(delight = delight),
        )

    /** Sortiert in-memory nach dem DAO-Order (pinned DESC, position ASC, updatedAt DESC). */
    private fun sortNotes(notes: List<Note>, sort: SortMode): List<Note> = when (sort) {
        SortMode.PINNWAND -> notes
        SortMode.CREATED -> notes.sortedWith(
            compareByDescending<Note> { it.pinned }.thenByDescending { it.createdAt },
        )
        SortMode.TITLE -> notes.sortedWith(
            compareByDescending<Note> { it.pinned }
                // Collator statt String-Vergleich, damit Umlaute korrekt einsortieren.
                .then(compareBy(Collator.getInstance(Locale.GERMAN)) { it.title.ifBlank { it.preview } }),
        )
    }

    fun onSearchChange(query: String) = searchQuery.update { query }

    fun clearSearch() = searchQuery.update { "" }

    fun toggleMoodFilter(mood: MoodCategory) =
        moodFilter.update { if (it == mood) null else mood }

    fun toggleTagFilter(tag: String) =
        tagFilter.update { if (it == tag) null else tag }

    fun toggleTag(note: Note, tag: String) = viewModelScope.launch {
        val current = note.tagList
        val next = if (tag in current) current - tag else current + tag
        repository.setTags(note.id, note.withTags(next).tags)
    }

    fun addTag(note: Note, tag: String) = viewModelScope.launch {
        repository.setTags(note.id, note.withTags(note.tagList + tag).tags)
    }

    /** Exportiert alle Notizen (inkl. Fotos & Verknüpfungen) als ZIP nach [uri]. */
    fun exportBackup(context: android.content.Context, uri: android.net.Uri, onResult: (Int) -> Unit) =
        viewModelScope.launch {
            onResult(
                runCatching {
                    com.papernotes.data.backup.BackupManager.export(context, repository, uri)
                }.getOrDefault(-1),
            )
        }

    fun importBackup(context: android.content.Context, uri: android.net.Uri, onResult: (Int) -> Unit) =
        viewModelScope.launch {
            onResult(
                runCatching {
                    com.papernotes.data.backup.BackupManager.import(context, repository, uri)
                }.getOrDefault(-1),
            )
        }

    /** Speichert die per Pinnwand gezogene Reihenfolge (Stapel-Mitglieder bleiben zusammenhängend). */
    fun applyOrder(items: List<GridItem>) = viewModelScope.launch {
        val ids = items.flatMap { item ->
            when (item) {
                is SoloItem -> listOf(item.gridNote.note.id)
                is StackItem -> item.members.map { it.note.id }
            }
        }
        repository.applyOrder(ids)
    }

    fun toggleDogEar(note: Note) = viewModelScope.launch {
        repository.setDogEar(note.id, folded = !note.dogEarFolded, mood = note.mood)
    }

    fun setMood(note: Note, mood: MoodCategory) = viewModelScope.launch {
        repository.setDogEar(note.id, folded = true, mood = mood)
    }

    fun togglePin(note: Note) = viewModelScope.launch {
        repository.setPinned(note.id, !note.pinned)
    }

    fun toggleSeal(note: Note) = viewModelScope.launch {
        repository.setSealed(note.id, !note.sealed)
    }

    fun toggleInvisibleInk(note: Note) = viewModelScope.launch {
        repository.setInvisibleInk(note.id, !note.invisibleInk)
    }

    fun toggleDone(note: Note) = viewModelScope.launch {
        repository.setDone(note.id, !note.done)
    }

    fun setReminder(note: Note, at: Long?, rule: ReminderRule = ReminderRule.NONE) =
        viewModelScope.launch {
            val newRule = if (at != null) rule else ReminderRule.NONE
            // Voll speichern, damit auch die Wiederholungs-Regel persistiert wird.
            repository.save(note.copy(reminderAt = at, reminderRule = newRule))
            if (at != null) {
                reminderScheduler.schedule(note.id, note.title, at, newRule)
            } else {
                reminderScheduler.cancel(note.id)
                reminderScheduler.dismissNotification(note.id)
            }
        }

    fun setCapsule(note: Note, at: Long?) = viewModelScope.launch {
        // Versiegeln + Selbst-Öffnen einplanen (bzw. auflösen).
        repository.save(note.copy(capsuleAt = at, sealed = if (at != null) true else note.sealed))
        if (at != null) reminderScheduler.scheduleCapsule(note.id, at)
        else reminderScheduler.cancelCapsule(note.id)
    }

    fun setExpiry(note: Note, at: Long?) = viewModelScope.launch {
        repository.setExpiry(note.id, at)
    }

    fun setCountdown(note: Note, at: Long?) = viewModelScope.launch {
        repository.setCountdown(note.id, at)
    }

    fun setPhoto(note: Note, path: String?) = viewModelScope.launch {
        repository.setPhoto(note.id, path)
    }

    fun setPaper(note: Note, style: PaperStyle) = viewModelScope.launch {
        repository.setPaper(note.id, style)
    }

    fun linkNotes(a: Long, b: Long) = viewModelScope.launch { repository.linkNotes(a, b) }

    fun unlinkNotes(a: Long, b: Long) = viewModelScope.launch { repository.unlinkNotes(a, b) }

    /**
     * Klammert [otherId] an den Stapel von [target] (bzw. löst sie wieder). Hat [target] noch
     * keinen Stapel, wird einer mit der eigenen Id als clipId eröffnet.
     */
    fun toggleClip(target: Note, otherId: Long) = viewModelScope.launch {
        val groupId = target.clipId ?: target.id
        if (target.clipId == null) repository.setClip(target.id, groupId)
        if (isInGroup(otherId, groupId)) {
            repository.setClip(otherId, null)
        } else {
            repository.setClip(otherId, groupId)
        }
    }

    /** Löst eine einzelne Notiz aus ihrem Stapel. */
    fun unclip(note: Note) = viewModelScope.launch { repository.setClip(note.id, null) }

    private fun isInGroup(id: Long, groupId: Long): Boolean =
        uiState.value.notes.any { it.note.id == id && it.note.clipId == groupId }

    /** Einmal-Event (Notiz-Id), wenn ein Stempel die Strähne auf eine 7er-Marke hebt. */
    private val _stampMilestone = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val stampMilestone: SharedFlow<Long> = _stampMilestone

    /** Setzt/entfernt den Stempel für [day] einer Stempelkarte direkt aus dem Grid. */
    fun toggleStamp(note: Note, day: Long) = viewModelScope.launch {
        val days = note.stamps.toMutableSet()
        val nowStamped = if (day in days) {
            days.remove(day); false
        } else {
            days.add(day); true
        }
        repository.save(note.withStamps(days))
        if (nowStamped) {
            val streak = StampCodec.streak(days, LocalDate.now().toEpochDay())
            if (streak > 0 && streak % 7L == 0L) _stampMilestone.tryEmit(note.id)
        }
    }

    fun archive(id: Long) = viewModelScope.launch {
        repository.archive(id)
        pushUndo(UndoKind.ARCHIVE, listOf(id))
    }

    fun restore(id: Long) = viewModelScope.launch { repository.restore(id) }

    fun moveToTrash(id: Long) = viewModelScope.launch {
        repository.moveToTrash(id)
        pushUndo(UndoKind.TRASH, listOf(id))
    }

    // Undo-Streifen: merkt sich nur die letzte Aktion (simpel & robust); eine neue Aktion
    // ersetzt das Event und startet den Auto-Dismiss-Timer über die neue Id neu.
    private var undoCounter = 0L
    private val _undoEvent = MutableStateFlow<UndoEvent?>(null)
    val undoEvent: StateFlow<UndoEvent?> = _undoEvent

    private fun pushUndo(kind: UndoKind, ids: List<Long>) {
        val n = ids.size
        val (message, action) = when (kind) {
            UndoKind.TRASH ->
                (if (n == 1) "Zettel zerknüllt" else "$n Zettel zerknüllt") to "Glattstreichen"
            UndoKind.ARCHIVE ->
                (if (n == 1) "In die Schublade gelegt" else "$n Zettel in die Schublade gelegt") to "Zurückholen"
        }
        _undoEvent.value = UndoEvent(++undoCounter, message, action, kind, ids)
    }

    fun undoLast() = viewModelScope.launch {
        val event = _undoEvent.value ?: return@launch
        _undoEvent.value = null
        event.noteIds.forEach { id ->
            when (event.kind) {
                UndoKind.TRASH -> repository.restore(id)
                // Bewusst unarchive statt restore: restore würde nebenbei expiresAt löschen.
                UndoKind.ARCHIVE -> repository.unarchive(id)
            }
        }
    }

    fun dismissUndo(id: Long) = _undoEvent.update { if (it?.id == id) null else it }

    fun markDelightPulled() = viewModelScope.launch { delightPreferences.markPulledToday() }

    private fun startOfToday(): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
