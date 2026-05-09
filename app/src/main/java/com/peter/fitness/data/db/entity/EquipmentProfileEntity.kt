package com.peter.fitness.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equipment_profile")
data class EquipmentProfileEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "bar_kg") val barKg: Double,
    @ColumnInfo(name = "has_rack") val hasRack: Boolean,
    @ColumnInfo(name = "has_bench") val hasBench: Boolean,
    @ColumnInfo(name = "has_pull_up_bar") val hasPullUpBar: Boolean,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {
    companion object {
        const val DEFAULT_ID = "default"
    }
}
