package com.papernotes.data.repository

import com.papernotes.data.local.NoteDao
import com.papernotes.data.local.toDomain
import com.papernotes.data.local.toEntity
import com.papernotes.domain.model.MoodCategory
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.NoteLink
import com.papernotes.domain.model.PaperStyle
import com.papernotes.domain.model.linkOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface NoteRepository {
    fun observeActiveNotes(): Flow<List<Note>>
    fun observeArchivedNotes(): Flow<List<Note>>
    fun observeTrashedNotes(): Flow<List<Note>>
    fun observeNote(id: Long): Flow<Note?>
    fun countCreatedSince(since: Long): Flow<Int>
    suspend fun getNote(id: Long): Note?
    suspend fun save(note: Note): Long
    suspend fun archive(id: Long)
    suspend fun unarchive(id: Long)
    suspend fun setPinned(id: Long, pinned: Boolean)
    suspend fun setSealed(id: Long, sealed: Boolean)
    suspend fun setInvisibleInk(id: Long, on: Boolean)
    suspend fun setDone(id: Long, done: Boolean)
    suspend fun setDogEar(id: Long, folded: Boolean, mood: MoodCategory)
    suspend fun setReminder(id: Long, at: Long?)
    suspend fun setExpiry(id: Long, at: Long?)
    suspend fun setClip(id: Long, clipId: Long?)
    suspend fun setCountdown(id: Long, at: Long?)
    suspend fun setPhoto(id: Long, path: String?)
    suspend fun setPaper(id: Long, paper: PaperStyle)
    suspend fun setTags(id: Long, tags: String)
    suspend fun applyOrder(orderedIds: List<Long>)
    suspend fun purgeExpired()
    suspend fun notesWithReminders(): List<Note>
    suspend fun notesWithCapsules(): List<Note>
    suspend fun moveToTrash(id: Long)
    suspend fun restore(id: Long)
    suspend fun moveToTrashMany(ids: List<Long>)
    suspend fun archiveMany(ids: List<Long>)
    suspend fun setPinnedMany(ids: List<Long>, pinned: Boolean)
    suspend fun setMoodMany(ids: List<Long>, mood: MoodCategory)
    suspend fun addTagMany(ids: List<Long>, tag: String)
    suspend fun purgeOldTrash()
    suspend fun delete(id: Long)
    fun observeLinks(): Flow<List<NoteLink>>
    suspend fun linkNotes(a: Long, b: Long)
    suspend fun unlinkNotes(a: Long, b: Long)
    suspend fun notesForBackup(): List<Note>
    suspend fun allLinks(): List<NoteLink>
    suspend fun insertRaw(note: Note): Long
}

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val dao: NoteDao,
) : NoteRepository {

    override fun observeActiveNotes(): Flow<List<Note>> =
        dao.observeActive().map { list -> list.map { it.toDomain() } }

    override fun observeArchivedNotes(): Flow<List<Note>> =
        dao.observeArchived().map { list -> list.map { it.toDomain() } }

    override fun observeTrashedNotes(): Flow<List<Note>> =
        dao.observeTrashed().map { list -> list.map { it.toDomain() } }

    override fun observeNote(id: Long): Flow<Note?> =
        dao.observeById(id).map { it?.toDomain() }

    override fun countCreatedSince(since: Long): Flow<Int> = dao.countCreatedSince(since)

    override suspend fun getNote(id: Long): Note? = dao.getById(id)?.toDomain()

    override suspend fun save(note: Note): Long {
        val now = System.currentTimeMillis()
        return dao.upsert(note.copy(updatedAt = now).toEntity())
    }

    override suspend fun archive(id: Long) =
        dao.setArchived(id, archived = true, now = System.currentTimeMillis())

    override suspend fun unarchive(id: Long) =
        dao.setArchived(id, archived = false, now = System.currentTimeMillis())

    override suspend fun setPinned(id: Long, pinned: Boolean) =
        dao.setPinned(id, pinned, System.currentTimeMillis())

    override suspend fun setSealed(id: Long, sealed: Boolean) =
        dao.setSealed(id, sealed, System.currentTimeMillis())

    override suspend fun setInvisibleInk(id: Long, on: Boolean) =
        dao.setInvisibleInk(id, on, System.currentTimeMillis())

    override suspend fun setDone(id: Long, done: Boolean) =
        dao.setDone(id, done, System.currentTimeMillis())

    override suspend fun setDogEar(id: Long, folded: Boolean, mood: MoodCategory) =
        dao.setDogEar(id, folded, mood.name, System.currentTimeMillis())

    override suspend fun setReminder(id: Long, at: Long?) =
        dao.setReminder(id, at, System.currentTimeMillis())

    override suspend fun setExpiry(id: Long, at: Long?) =
        dao.setExpiry(id, at, System.currentTimeMillis())

    override suspend fun setClip(id: Long, clipId: Long?) =
        dao.setClip(id, clipId, System.currentTimeMillis())

    override suspend fun setCountdown(id: Long, at: Long?) =
        dao.setCountdown(id, at, System.currentTimeMillis())

    override suspend fun setPhoto(id: Long, path: String?) =
        dao.setPhoto(id, path, System.currentTimeMillis())

    override suspend fun setPaper(id: Long, paper: PaperStyle) =
        dao.setPaper(id, paper.name, System.currentTimeMillis())

    override suspend fun setTags(id: Long, tags: String) =
        dao.setTags(id, tags, System.currentTimeMillis())

    override suspend fun applyOrder(orderedIds: List<Long>) =
        dao.applyOrder(orderedIds)

    override suspend fun purgeExpired() =
        dao.purgeExpired(System.currentTimeMillis())

    override suspend fun notesWithReminders(): List<Note> =
        dao.notesWithReminders().map { it.toDomain() }

    override suspend fun notesWithCapsules(): List<Note> =
        dao.notesWithCapsules().map { it.toDomain() }

    override suspend fun moveToTrash(id: Long) =
        dao.moveToTrash(id, System.currentTimeMillis())

    override suspend fun restore(id: Long) =
        dao.restore(id, System.currentTimeMillis())

    override suspend fun moveToTrashMany(ids: List<Long>) =
        dao.moveToTrashMany(ids, System.currentTimeMillis())

    override suspend fun archiveMany(ids: List<Long>) =
        dao.setArchivedMany(ids, archived = true, now = System.currentTimeMillis())

    override suspend fun setPinnedMany(ids: List<Long>, pinned: Boolean) =
        dao.setPinnedMany(ids, pinned, System.currentTimeMillis())

    override suspend fun setMoodMany(ids: List<Long>, mood: MoodCategory) =
        dao.setMoodMany(ids, mood.name, System.currentTimeMillis())

    override suspend fun addTagMany(ids: List<Long>, tag: String) {
        // Additiv je Notiz (Tags sind ein zusammengesetztes Feld, kein reines Spalten-Update).
        val now = System.currentTimeMillis()
        ids.forEach { id ->
            val note = dao.getById(id)?.toDomain() ?: return@forEach
            if (tag !in note.tagList) dao.setTags(id, note.withTags(note.tagList + tag).tags, now)
        }
    }

    override suspend fun purgeOldTrash() {
        dao.purgeTrash(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30))
        dao.purgeOrphanLinks()
    }

    override suspend fun delete(id: Long) {
        dao.deleteLinksFor(id)
        dao.delete(id)
    }

    override fun observeLinks(): Flow<List<NoteLink>> =
        dao.observeLinks().map { list -> list.map { it.toDomain() } }

    override suspend fun linkNotes(a: Long, b: Long) =
        dao.insertLink(linkOf(a, b).toEntity())

    override suspend fun unlinkNotes(a: Long, b: Long) {
        val link = linkOf(a, b)
        dao.deleteLink(link.aId, link.bId)
    }

    override suspend fun notesForBackup(): List<Note> =
        dao.notesForBackup().map { it.toDomain() }

    override suspend fun allLinks(): List<NoteLink> =
        dao.allLinks().map { it.toDomain() }

    override suspend fun insertRaw(note: Note): Long =
        dao.insert(note.copy(id = 0L).toEntity())
}
