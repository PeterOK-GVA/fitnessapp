package com.peter.fitness.data.db.mapper

import com.peter.fitness.data.db.entity.EquipmentProfileEntity
import com.peter.fitness.data.db.entity.PlatePairEntity
import com.peter.fitness.domain.model.EquipmentInventory
import com.peter.fitness.domain.model.PlatePair
import java.time.Instant

fun EquipmentProfileEntity.toDomain(plates: List<PlatePairEntity>): EquipmentInventory =
    EquipmentInventory(
        barKg = barKg,
        hasRack = hasRack,
        hasBench = hasBench,
        hasPullUpBar = hasPullUpBar,
        plates = plates.map(PlatePairEntity::toDomain),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

fun EquipmentInventory.toProfileEntity(id: String = EquipmentProfileEntity.DEFAULT_ID): EquipmentProfileEntity =
    EquipmentProfileEntity(
        id = id,
        barKg = barKg,
        hasRack = hasRack,
        hasBench = hasBench,
        hasPullUpBar = hasPullUpBar,
        updatedAt = updatedAt.toEpochMilli(),
    )

fun EquipmentInventory.toPlateEntities(profileId: String = EquipmentProfileEntity.DEFAULT_ID): List<PlatePairEntity> =
    plates.map { it.toEntity(profileId) }

fun PlatePairEntity.toDomain(): PlatePair = PlatePair(
    denominationKg = denominationKg,
    pairCount = pairCount,
)

fun PlatePair.toEntity(profileId: String): PlatePairEntity = PlatePairEntity(
    profileId = profileId,
    denominationKg = denominationKg,
    pairCount = pairCount,
)
