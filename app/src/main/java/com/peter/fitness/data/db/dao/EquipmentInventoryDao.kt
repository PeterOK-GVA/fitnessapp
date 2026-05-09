package com.peter.fitness.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.peter.fitness.data.db.entity.EquipmentProfileEntity
import com.peter.fitness.data.db.entity.PlatePairEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentInventoryDao {

    @Query("SELECT * FROM equipment_profile WHERE id = :id")
    suspend fun findProfile(id: String = EquipmentProfileEntity.DEFAULT_ID): EquipmentProfileEntity?

    @Query("SELECT * FROM equipment_profile WHERE id = :id")
    fun observeProfile(id: String = EquipmentProfileEntity.DEFAULT_ID): Flow<EquipmentProfileEntity?>

    @Query("SELECT * FROM plate_pair WHERE profile_id = :profileId ORDER BY denomination_kg DESC")
    fun observePlates(profileId: String = EquipmentProfileEntity.DEFAULT_ID): Flow<List<PlatePairEntity>>

    @Query("SELECT * FROM plate_pair WHERE profile_id = :profileId ORDER BY denomination_kg DESC")
    suspend fun findPlates(profileId: String = EquipmentProfileEntity.DEFAULT_ID): List<PlatePairEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: EquipmentProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlates(plates: List<PlatePairEntity>)

    @Query("DELETE FROM plate_pair WHERE profile_id = :profileId")
    suspend fun deletePlates(profileId: String)

    @Transaction
    suspend fun replacePlates(profileId: String, plates: List<PlatePairEntity>) {
        deletePlates(profileId)
        upsertPlates(plates)
    }
}
