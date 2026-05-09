package com.peter.fitness.testsupport

import com.peter.fitness.domain.model.EquipmentInventory
import com.peter.fitness.domain.repository.EquipmentInventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

class FakeEquipmentInventoryRepository(
    initial: EquipmentInventory = EquipmentInventory(
        barKg = 20.0,
        hasRack = true,
        hasBench = true,
        hasPullUpBar = false,
        plates = emptyList(),
        updatedAt = Instant.EPOCH,
    ),
) : EquipmentInventoryRepository {

    private val _state = MutableStateFlow(initial)
    var updateCount: Int = 0
        private set

    override fun observeCurrent(): Flow<EquipmentInventory> = _state.asStateFlow()

    override suspend fun current(): EquipmentInventory = _state.value

    override suspend fun update(inventory: EquipmentInventory) {
        _state.value = inventory
        updateCount += 1
    }
}
