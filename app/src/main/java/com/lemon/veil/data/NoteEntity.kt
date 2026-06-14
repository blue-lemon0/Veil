package com.lemon.veil.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalText: String,
    val suggestion: String,
    val type: String = "任务",
    @ColumnInfo(name = "suggestedTime")
    val time: Long?,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val finalAction: String? = null,
    val location: String = "",
    val calendarEventId: Long? = null,
    val isCalendarSynced: Boolean = false,
    val hasAlarm: Boolean = false,
    val orderIndex: Int = 0,
    val parentId: Long? = null,
    val stepOrder: Int = 0,
    val currentStepIndex: Int = 0
) {
    @Ignore
    var steps: List<NoteEntity> = emptyList()
}
