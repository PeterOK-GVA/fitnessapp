package com.peter.fitness.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plate_pair",
    foreignKeys = [
        ForeignKey(
            entity = EquipmentProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["profile_id", "denomination_kg"], unique = true),
        Index("profile_id"),
    ],
)
data class PlatePairEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "profile_id") val profileId: String,
    @ColumnInfo(name = "denomination_kg") val denominationKg: Double,
    @ColumnInfo(name = "pair_count") val pairCount: Int,
)
