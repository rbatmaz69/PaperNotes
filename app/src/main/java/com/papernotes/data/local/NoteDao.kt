package com.papernotes.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query(
        "SELECT * FROM notes WHERE archived = 0 AND deletedAt IS NULL " +
            "ORDER BY pinned DESC, position ASC, updatedAt DESC",
    )
    fun observeActive(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE archived = 1 AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeArchived(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrashed(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    /** Alle aktiven & archivierten Notizen (ohne Papierkorb) für die Sicherung. */
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL")
    suspend fun notesForBackup(): List<NoteEntity>

    @Query("SELECT * FROM note_links")
    suspend fun allLinks(): List<NoteLinkEntity>

    @Query("SELECT COUNT(*) FROM notes WHERE createdAt >= :since AND deletedAt IS NULL")
    fun countCreatedSince(since: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Upsert
    suspend fun upsert(note: NoteEntity): Long

    @Query("UPDATE notes SET archived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, now: Long)

    @Query("UPDATE notes SET pinned = :pinned, updatedAt = :now WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, now: Long)

    @Query("UPDATE notes SET sealed = :sealed, updatedAt = :now WHERE id = :id")
    suspend fun setSealed(id: Long, sealed: Boolean, now: Long)

    @Query("UPDATE notes SET invisibleInk = :on, updatedAt = :now WHERE id = :id")
    suspend fun setInvisibleInk(id: Long, on: Boolean, now: Long)

    @Query("UPDATE notes SET done = :done, updatedAt = :now WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, now: Long)

    @Query("UPDATE notes SET dogEarFolded = :folded, mood = :mood, updatedAt = :now WHERE id = :id")
    suspend fun setDogEar(id: Long, folded: Boolean, mood: String, now: Long)

    @Query("UPDATE notes SET reminderAt = :at, updatedAt = :now WHERE id = :id")
    suspend fun setReminder(id: Long, at: Long?, now: Long)

    @Query("UPDATE notes SET expiresAt = :at, updatedAt = :now WHERE id = :id")
    suspend fun setExpiry(id: Long, at: Long?, now: Long)

    @Query("UPDATE notes SET clipId = :clipId, updatedAt = :now WHERE id = :id")
    suspend fun setClip(id: Long, clipId: Long?, now: Long)

    @Query("UPDATE notes SET countdownAt = :at, updatedAt = :now WHERE id = :id")
    suspend fun setCountdown(id: Long, at: Long?, now: Long)

    @Query("UPDATE notes SET photoPath = :path, updatedAt = :now WHERE id = :id")
    suspend fun setPhoto(id: Long, path: String?, now: Long)

    @Query("UPDATE notes SET paper = :paper, updatedAt = :now WHERE id = :id")
    suspend fun setPaper(id: Long, paper: String, now: Long)

    @Query("UPDATE notes SET tags = :tags, updatedAt = :now WHERE id = :id")
    suspend fun setTags(id: Long, tags: String, now: Long)

    @Query("UPDATE notes SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Long)

    /** Schreibt die manuelle Reihenfolge: position = Index in [orderedIds]. */
    @Transaction
    suspend fun applyOrder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { i, id -> setPosition(id, i.toLong()) }
    }

    /** Abgelaufene Notizen still in den Papierkorb verschieben (Ablaufzeit dabei leeren). */
    @Query(
        "UPDATE notes SET deletedAt = :now, expiresAt = NULL " +
            "WHERE expiresAt IS NOT NULL AND expiresAt <= :now AND deletedAt IS NULL",
    )
    suspend fun purgeExpired(now: Long)

    @Query("SELECT * FROM notes WHERE reminderAt IS NOT NULL AND deletedAt IS NULL")
    suspend fun notesWithReminders(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE capsuleAt IS NOT NULL AND sealed = 1 AND deletedAt IS NULL")
    suspend fun notesWithCapsules(): List<NoteEntity>

    @Query("UPDATE notes SET deletedAt = :now, expiresAt = NULL, clipId = NULL WHERE id = :id")
    suspend fun moveToTrash(id: Long, now: Long)

    @Query(
        "UPDATE notes SET deletedAt = NULL, archived = 0, expiresAt = NULL, updatedAt = :now " +
            "WHERE id = :id",
    )
    suspend fun restore(id: Long, now: Long)

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeTrash(cutoff: Long)

    // --- Batch-Operationen (Mehrfachauswahl im Grid) ---

    @Query("UPDATE notes SET deletedAt = :now, expiresAt = NULL, clipId = NULL WHERE id IN (:ids)")
    suspend fun moveToTrashMany(ids: List<Long>, now: Long)

    @Query("UPDATE notes SET archived = :archived, updatedAt = :now WHERE id IN (:ids)")
    suspend fun setArchivedMany(ids: List<Long>, archived: Boolean, now: Long)

    @Query("UPDATE notes SET pinned = :pinned, updatedAt = :now WHERE id IN (:ids)")
    suspend fun setPinnedMany(ids: List<Long>, pinned: Boolean, now: Long)

    @Query("UPDATE notes SET dogEarFolded = 1, mood = :mood, updatedAt = :now WHERE id IN (:ids)")
    suspend fun setMoodMany(ids: List<Long>, mood: String, now: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    // --- Verknüpfungen ("roter Faden") ---

    @Query("SELECT * FROM note_links")
    fun observeLinks(): Flow<List<NoteLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLink(link: NoteLinkEntity)

    @Query("DELETE FROM note_links WHERE aId = :a AND bId = :b")
    suspend fun deleteLink(a: Long, b: Long)

    @Query("DELETE FROM note_links WHERE aId = :id OR bId = :id")
    suspend fun deleteLinksFor(id: Long)

    @Query(
        "DELETE FROM note_links WHERE aId NOT IN (SELECT id FROM notes) " +
            "OR bId NOT IN (SELECT id FROM notes)",
    )
    suspend fun purgeOrphanLinks()
}
