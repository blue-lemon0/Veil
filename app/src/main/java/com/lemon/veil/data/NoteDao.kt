package com.lemon.veil.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    suspend fun getAllNotesOnce(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE parentId IS NULL ORDER BY CASE WHEN suggestedTime IS NULL THEN 1 ELSE 0 END, suggestedTime ASC, createdAt DESC")
    fun getRootNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE parentId = :parentId ORDER BY stepOrder ASC, createdAt ASC")
    fun getStepsByParentId(parentId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE parentId = :parentId ORDER BY stepOrder ASC, createdAt ASC")
    suspend fun getStepsOnce(parentId: Long): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): NoteEntity?

    @Query("SELECT COUNT(*) FROM notes WHERE parentId = :parentId")
    suspend fun getStepCount(parentId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Transaction
    suspend fun insertTask(parent: NoteEntity): Long {
        val parentId = insertNote(parent)
        for (step in parent.steps) {
            insertNote(step.copy(parentId = parentId))
        }
        return parentId
    }

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("UPDATE notes SET calendarEventId = :eventId, isCalendarSynced = :isSynced WHERE id = :noteId")
    suspend fun updateSyncStatus(noteId: Long, eventId: Long?, isSynced: Boolean)

    @Query("UPDATE notes SET hasAlarm = :hasAlarm WHERE id = :noteId")
    suspend fun updateAlarmStatus(noteId: Long, hasAlarm: Boolean)

    @Query("UPDATE notes SET isCalendarSynced = 0 WHERE calendarEventId = :eventId")
    suspend fun clearSyncStatus(eventId: Long)

    @Query("UPDATE notes SET suggestedTime = :time WHERE id = :noteId")
    suspend fun updateSuggestedTime(noteId: Long, time: Long?)

    @Query("UPDATE notes SET finalAction = :action WHERE id = :noteId")
    suspend fun updateActionField(noteId: Long, action: String)

    @Query("UPDATE notes SET location = :location WHERE id = :noteId")
    suspend fun updateLocationField(noteId: Long, location: String)

    @Query("UPDATE notes SET stepOrder = :stepOrder WHERE id = :noteId")
    suspend fun updateStepOrder(noteId: Long, stepOrder: Int)

    @Query("UPDATE notes SET currentStepIndex = :index WHERE id = :noteId")
    suspend fun updateStepIndex(noteId: Long, index: Int)

    @Query("UPDATE notes SET isCompleted = :completed WHERE id = :noteId")
    suspend fun updateCompleted(noteId: Long, completed: Boolean)

    @Query("UPDATE notes SET parentId = :parentId, stepOrder = :stepOrder WHERE id = :noteId")
    suspend fun updateNoteParent(noteId: Long, parentId: Long?, stepOrder: Int)

    @Query("UPDATE notes SET parentId = NULL, stepOrder = 0 WHERE id IN (:noteIds)")
    suspend fun promoteToRoot(noteIds: List<Long>)

    @Transaction
    suspend fun mergeNotes(noteIds: List<Long>, newTitle: String): Long {
        val parentId = insertNote(NoteEntity(originalText = newTitle, suggestion = newTitle, time = null))
        noteIds.forEachIndexed { index, id -> updateNoteParent(id, parentId, index) }
        return parentId
    }

    @Transaction
    suspend fun setAsSubTasks(noteIds: List<Long>, targetId: Long) {
        val count = getStepCount(targetId)
        noteIds.forEachIndexed { index, id -> updateNoteParent(id, targetId, count + index) }
    }
}
