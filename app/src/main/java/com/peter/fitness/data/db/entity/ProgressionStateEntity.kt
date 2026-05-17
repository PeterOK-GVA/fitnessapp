package com.peter.fitness.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progression_state")
data class ProgressionStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "current_load_kg") val currentLoadKg: Double,
    @ColumnInfo(name = "current_target_reps") val currentTargetReps: Int,
    @ColumnInfo(name = "working_range_min_reps") val workingRangeMinReps: Int,
    @ColumnInfo(name = "working_range_max_reps") val workingRangeMaxReps: Int,
    @ColumnInfo(name = "last_stimulus") val lastStimulus: String?,
    @ColumnInfo(name = "volume_streak") val volumeStreak: Int,
    @ColumnInfo(name = "consecutive_successes") val consecutiveSuccesses: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
