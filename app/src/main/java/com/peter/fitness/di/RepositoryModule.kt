package com.peter.fitness.di

import com.peter.fitness.data.repository.RoomEquipmentInventoryRepository
import com.peter.fitness.data.repository.RoomExerciseRepository
import com.peter.fitness.data.repository.RoomRestTimerRepository
import com.peter.fitness.data.repository.RoomSessionRepository
import com.peter.fitness.domain.repository.EquipmentInventoryRepository
import com.peter.fitness.domain.repository.ExerciseRepository
import com.peter.fitness.domain.repository.RestTimerRepository
import com.peter.fitness.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExerciseRepository(impl: RoomExerciseRepository): ExerciseRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: RoomSessionRepository): SessionRepository

    @Binds
    @Singleton
    abstract fun bindEquipmentInventoryRepository(impl: RoomEquipmentInventoryRepository): EquipmentInventoryRepository

    @Binds
    @Singleton
    abstract fun bindRestTimerRepository(impl: RoomRestTimerRepository): RestTimerRepository
}
