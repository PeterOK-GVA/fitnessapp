package com.peter.fitness.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.peter.fitness.data.db.entity.SessionEntity
import com.peter.fitness.data.db.entity.SetEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM session ORDER BY started_at DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM session WHERE id = :id")
    suspend fun findById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: SessionEntity)

    @Update
    suspend fun update(session: SessionEntity)

    @Query("DELETE FROM session WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM set_entry WHERE session_id = :sessionId ORDER BY ordinal")
    fun observeSetEntries(sessionId: String): Flow<List<SetEntryEntity>>

    @Query("SELECT * FROM set_entry ORDER BY created_at")
    fun observeAllSets(): Flow<List<SetEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSetEntry(setEntry: SetEntryEntity)

    @Update
    suspend fun updateSetEntry(setEntry: SetEntryEntity)

    @Delete
    suspend fun deleteSetEntry(setEntry: SetEntryEntity)

    @Query("DELETE FROM set_entry WHERE id = :id")
    suspend fun deleteSetEntryById(id: String)
}
