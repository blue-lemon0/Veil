package com.lemon.veil.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<NoteEntity>>
    suspend fun getAllNotesOnce(): List<NoteEntity>
    fun getRootNotes(): Flow<List<NoteEntity>>
    fun getStepsByParentId(parentId: Long): Flow<List<NoteEntity>>
    suspend fun getStepsOnce(parentId: Long): List<NoteEntity>
    suspend fun getNoteById(noteId: Long): NoteEntity?
    suspend fun getStepCount(parentId: Long): Int
    suspend fun insertNote(note: NoteEntity): Long
    suspend fun insertTask(parent: NoteEntity): Long
    suspend fun deleteNote(note: NoteEntity)
    suspend fun updateAlarmStatus(noteId: Long, hasAlarm: Boolean)
    suspend fun updateSyncStatus(noteId: Long, eventId: Long?, isSynced: Boolean)
    suspend fun updateSuggestedTime(noteId: Long, time: Long?)
    suspend fun updateActionField(noteId: Long, action: String)
    suspend fun updateLocationField(noteId: Long, location: String)
    suspend fun updateStepOrder(noteId: Long, stepOrder: Int)
    suspend fun updateStepIndex(noteId: Long, index: Int)
    suspend fun updateCompleted(noteId: Long, completed: Boolean)
    suspend fun updateNoteParent(noteId: Long, parentId: Long?, stepOrder: Int)
    suspend fun mergeNotes(noteIds: List<Long>, newTitle: String): Long
    suspend fun setAsSubTasks(noteIds: List<Long>, targetId: Long)
    suspend fun promoteToRoot(noteIds: List<Long>)
    suspend fun updateHabitStack(noteId: Long, currentHabit: String, newHabit: String)

    suspend fun updateHabitDesign(
        noteId: Long,
        cue: String, craving: String, responsePlan: String, reward: String,
        badCue: String, badCraving: String, badResponsePlan: String, badReward: String
    )
}

class NoteRepositoryImpl(context: Context) : NoteRepository {
    private val dao = AppDatabase.getDatabase(context).noteDao()

    override fun getAllNotes(): Flow<List<NoteEntity>> = dao.getAllNotes()
    override suspend fun getAllNotesOnce(): List<NoteEntity> = dao.getAllNotesOnce()
    override fun getRootNotes(): Flow<List<NoteEntity>> = dao.getRootNotes()
    override fun getStepsByParentId(parentId: Long): Flow<List<NoteEntity>> = dao.getStepsByParentId(parentId)
    override suspend fun getStepsOnce(parentId: Long): List<NoteEntity> = dao.getStepsOnce(parentId)
    override suspend fun getNoteById(noteId: Long): NoteEntity? = dao.getNoteById(noteId)
    override suspend fun getStepCount(parentId: Long): Int = dao.getStepCount(parentId)
    override suspend fun insertNote(note: NoteEntity): Long = dao.insertNote(note)
    override suspend fun insertTask(parent: NoteEntity): Long = dao.insertTask(parent)
    override suspend fun deleteNote(note: NoteEntity) = dao.deleteNote(note)
    override suspend fun updateAlarmStatus(noteId: Long, hasAlarm: Boolean) =
        dao.updateAlarmStatus(noteId, hasAlarm)
    override suspend fun updateSyncStatus(noteId: Long, eventId: Long?, isSynced: Boolean) =
        dao.updateSyncStatus(noteId, eventId, isSynced)
    override suspend fun updateSuggestedTime(noteId: Long, time: Long?) = dao.updateSuggestedTime(noteId, time)
    override suspend fun updateActionField(noteId: Long, action: String) = dao.updateActionField(noteId, action)
    override suspend fun updateLocationField(noteId: Long, location: String) = dao.updateLocationField(noteId, location)
    override suspend fun updateStepOrder(noteId: Long, stepOrder: Int) = dao.updateStepOrder(noteId, stepOrder)
    override suspend fun updateStepIndex(noteId: Long, index: Int) = dao.updateStepIndex(noteId, index)
    override suspend fun updateCompleted(noteId: Long, completed: Boolean) = dao.updateCompleted(noteId, completed)
    override suspend fun updateNoteParent(noteId: Long, parentId: Long?, stepOrder: Int) =
        dao.updateNoteParent(noteId, parentId, stepOrder)
    override suspend fun mergeNotes(noteIds: List<Long>, newTitle: String): Long = dao.mergeNotes(noteIds, newTitle)
    override suspend fun setAsSubTasks(noteIds: List<Long>, targetId: Long) = dao.setAsSubTasks(noteIds, targetId)
    override suspend fun promoteToRoot(noteIds: List<Long>) = dao.promoteToRoot(noteIds)
    override suspend fun updateHabitStack(noteId: Long, currentHabit: String, newHabit: String) =
        dao.updateHabitStack(noteId, currentHabit, newHabit)

    override suspend fun updateHabitDesign(
        noteId: Long,
        cue: String, craving: String, responsePlan: String, reward: String,
        badCue: String, badCraving: String, badResponsePlan: String, badReward: String
    ) = dao.updateHabitDesign(noteId, cue, craving, responsePlan, reward, badCue, badCraving, badResponsePlan, badReward)
}
