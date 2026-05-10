package com.peter.fitness.di

import android.content.Context
import androidx.room.Room
import com.peter.fitness.data.db.FitnessDatabase
import com.peter.fitness.data.db.dao.EquipmentInventoryDao
import com.peter.fitness.data.db.dao.ExerciseDao
import com.peter.fitness.data.db.dao.RestTimerDao
import com.peter.fitness.data.db.dao.SessionDao
import com.peter.fitness.data.db.migrations.MIGRATION_1_2
import com.peter.fitness.data.db.seed.BarbellSeedCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFitnessDatabase(@ApplicationContext context: Context): FitnessDatabase =
        Room.databaseBuilder(context, FitnessDatabase::class.java, FitnessDatabase.NAME)
            .addCallback(BarbellSeedCallback)
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideExerciseDao(db: FitnessDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideSessionDao(db: FitnessDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideEquipmentInventoryDao(db: FitnessDatabase): EquipmentInventoryDao = db.equipmentInventoryDao()

    @Provides
    fun provideRestTimerDao(db: FitnessDatabase): RestTimerDao = db.restTimerDao()
}
