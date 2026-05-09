package com.peter.fitness.data.db.mapper

import com.peter.fitness.data.db.entity.EquipmentProfileEntity
import com.peter.fitness.domain.model.EquipmentInventory
import com.peter.fitness.domain.model.PlatePair
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class EquipmentInventoryMapperTest {

    @Test
    fun `round-trips inventory through profile + plate entities`() {
        val original = EquipmentInventory(
            barKg = 20.0,
            hasRack = true,
            hasBench = true,
            hasPullUpBar = false,
            plates = listOf(
                PlatePair(20.0, 4),
                PlatePair(10.0, 4),
                PlatePair(2.5, 4),
                PlatePair(1.25, 2),
            ),
            updatedAt = Instant.ofEpochMilli(1_700_000_000_000L),
        )
        val profile = original.toProfileEntity()
        val plates = original.toPlateEntities()
        val roundTripped = profile.toDomain(plates)
        roundTripped shouldBe original
    }

    @Test
    fun `default profile id is used when none supplied`() {
        val inventory = EquipmentInventory(
            barKg = 20.0,
            hasRack = true,
            hasBench = false,
            hasPullUpBar = false,
            plates = emptyList(),
            updatedAt = Instant.now(),
        )
        inventory.toProfileEntity().id shouldBe EquipmentProfileEntity.DEFAULT_ID
    }
}
