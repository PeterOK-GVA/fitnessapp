package com.peter.fitness.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.peter.fitness.data.db.entity.ProgressionStateEntity

@Dao
interface ProgressionStateDao {

    @Query("SELECT * FROM progression_state WHERE exercise_id = :exerciseId LIMIT 1")
    suspend fun find(exerciseId: String): ProgressionStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProgressionStateEntity)

    @Query("DELETE FROM progression_state WHERE exercise_id = :exerciseId")
    suspend fun delete(exerciseId: String)
}
