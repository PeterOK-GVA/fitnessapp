package com.peter.fitness.domain.repository

import com.peter.fitness.domain.model.EquipmentInventory
import kotlinx.coroutines.flow.Flow

interface EquipmentInventoryRepository {
    fun observeCurrent(): Flow<EquipmentInventory>

    suspend fun current(): EquipmentInventory

    suspend fun update(inventory: EquipmentInventory)
}
