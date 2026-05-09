package com.peter.fitness.data.repository

import com.peter.fitness.core.coroutines.IoDispatcher
import com.peter.fitness.data.db.dao.EquipmentInventoryDao
import com.peter.fitness.data.db.entity.EquipmentProfileEntity
import com.peter.fitness.data.db.mapper.toDomain
import com.peter.fitness.data.db.mapper.toPlateEntities
import com.peter.fitness.data.db.mapper.toProfileEntity
import com.peter.fitness.domain.model.EquipmentInventory
import com.peter.fitness.domain.repository.EquipmentInventoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class RoomEquipmentInventoryRepository @Inject constructor(
    private val dao: EquipmentInventoryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : EquipmentInventoryRepository {

    override fun observeCurrent(): Flow<EquipmentInventory> =
        combine(
            dao.observeProfile().mapNotNull { it },
            dao.observePlates(),
        ) { profile, plates -> profile.toDomain(plates) }.flowOn(ioDispatcher)

    override suspend fun current(): EquipmentInventory = withContext(ioDispatcher) {
        val profile = dao.findProfile() ?: error("Default equipment profile missing — seeder did not run")
        val plates = dao.findPlates()
        profile.toDomain(plates)
    }

    override suspend fun update(inventory: EquipmentInventory) {
        withContext(ioDispatcher) {
            val now = Instant.now()
            val toPersist = inventory.copy(updatedAt = now)
            dao.upsertProfile(toPersist.toProfileEntity())
            dao.replacePlates(EquipmentProfileEntity.DEFAULT_ID, toPersist.toPlateEntities())
        }
    }
}
