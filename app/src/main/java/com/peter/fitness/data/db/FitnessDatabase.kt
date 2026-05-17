package com.peter.fitness.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.peter.fitness.data.db.dao.EquipmentInventoryDao
import com.peter.fitness.data.db.dao.ExerciseDao
import com.peter.fitness.data.db.dao.ProgressionStateDao
import com.peter.fitness.data.db.dao.RestTimerDao
import com.peter.fitness.data.db.dao.SessionDao
import com.peter.fitness.data.db.entity.ActiveRestTimerEntity
import com.peter.fitness.data.db.entity.EquipmentProfileEntity
import com.peter.fitness.data.db.entity.ExerciseEntity
import com.peter.fitness.data.db.entity.PlatePairEntity
import com.peter.fitness.data.db.entity.ProgressionStateEntity
import com.peter.fitness.data.db.entity.SessionEntity
import com.peter.fitness.data.db.entity.SetEntryEntity

@Database(
    entities = [
        ExerciseEntity::class,
        SessionEntity::class,
        SetEntryEntity::class,
        EquipmentProfileEntity::class,
        PlatePairEntity::class,
        ActiveRestTimerEntity::class,
        ProgressionStateEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun sessionDao(): SessionDao
    abstract fun equipmentInventoryDao(): EquipmentInventoryDao
    abstract fun restTimerDao(): RestTimerDao
    abstract fun progressionStateDao(): ProgressionStateDao

    companion object {
        const val NAME = "fitness.db"
    }
}
