package com.peter.fitness.domain.repository

import com.peter.fitness.domain.model.RestTimer
import kotlinx.coroutines.flow.Flow

interface RestTimerRepository {
    fun observeActive(): Flow<RestTimer?>

    suspend fun current(): RestTimer?

    suspend fun start(timer: RestTimer)

    suspend fun cancel()
}
