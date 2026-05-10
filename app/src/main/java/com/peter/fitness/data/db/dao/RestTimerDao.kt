package com.peter.fitness.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.peter.fitness.data.db.entity.ActiveRestTimerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RestTimerDao {

    @Query("SELECT * FROM active_rest_timer WHERE id = :id LIMIT 1")
    fun observeActive(id: String = ActiveRestTimerEntity.ROW_ID): Flow<ActiveRestTimerEntity?>

    @Query("SELECT * FROM active_rest_timer WHERE id = :id LIMIT 1")
    suspend fun current(id: String = ActiveRestTimerEntity.ROW_ID): ActiveRestTimerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ActiveRestTimerEntity)

    @Query("DELETE FROM active_rest_timer WHERE id = :id")
    suspend fun clear(id: String = ActiveRestTimerEntity.ROW_ID)
}
