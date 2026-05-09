package com.peter.fitness.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "movement_pattern") val movementPattern: String,
    @ColumnInfo(name = "load_type") val loadType: String,
    @ColumnInfo(name = "technique_demand") val techniqueDemand: String,
    @ColumnInfo(name = "conditioning_suitability") val conditioningSuitability: String,
    @ColumnInfo(name = "requires_rack") val requiresRack: Boolean,
    @ColumnInfo(name = "requires_bench") val requiresBench: Boolean,
    @ColumnInfo(name = "requires_pull_up_bar") val requiresPullUpBar: Boolean,
    @ColumnInfo(name = "bar_weight_aware") val barWeightAware: Boolean,
    @ColumnInfo(name = "supports_tempo") val supportsTempo: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
