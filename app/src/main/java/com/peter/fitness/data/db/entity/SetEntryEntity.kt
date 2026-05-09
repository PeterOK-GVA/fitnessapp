package com.peter.fitness.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "set_entry",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("session_id"),
        Index("exercise_id"),
    ],
)
data class SetEntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    val ordinal: Int,
    @ColumnInfo(name = "target_reps") val targetReps: Int,
    @ColumnInfo(name = "target_load_kg") val targetLoadKg: Double,
    @ColumnInfo(name = "completed_reps") val completedReps: Int?,
    @ColumnInfo(name = "performed_load_kg") val performedLoadKg: Double?,
    @ColumnInfo(name = "subjective_load") val subjectiveLoad: String?,
    @ColumnInfo(name = "technique_rating") val techniqueRating: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
