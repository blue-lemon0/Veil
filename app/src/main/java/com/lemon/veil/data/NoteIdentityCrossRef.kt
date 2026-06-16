package com.lemon.veil.data

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "note_identities",
    primaryKeys = ["noteId", "identityId"],
    foreignKeys = [
        ForeignKey(
            entity = IdentityEntity::class,
            parentColumns = ["id"],
            childColumns = ["identityId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ]
)
data class NoteIdentityCrossRef(
    val noteId: Long,
    val identityId: Long,
)
