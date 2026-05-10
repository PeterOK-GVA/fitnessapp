package com.peter.fitness.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_rest_timer")
data class ActiveRestTimerEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Long,
    @ColumnInfo(name = "session_id") val sessionId: String?,
    @ColumnInfo(name = "set_entry_id") val setEntryId: String?,
    val label: String?,
) {
    companion object {
        const val ROW_ID = "current"
    }
}
