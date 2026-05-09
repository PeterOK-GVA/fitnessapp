package com.peter.fitness.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class EquipmentInventoryTest {

    @Test
    fun `negative bar weight is rejected`() {
        shouldThrow<IllegalArgumentException> {
            EquipmentInventory(
                barKg = -1.0,
                hasRack = true,
                hasBench = true,
                hasPullUpBar = false,
                plates = emptyList(),
                updatedAt = Instant.now(),
            )
        }
    }

    @Test
    fun `non-positive plate denomination is rejected`() {
        shouldThrow<IllegalArgumentException> { PlatePair(denominationKg = 0.0, pairCount = 4) }
        shouldThrow<IllegalArgumentException> { PlatePair(denominationKg = -1.25, pairCount = 4) }
    }

    @Test
    fun `negative pair count is rejected`() {
        shouldThrow<IllegalArgumentException> { PlatePair(denominationKg = 20.0, pairCount = -1) }
    }

    @Test
    fun `smallest plate ignores zero-count pairs`() {
        val inventory = EquipmentInventory(
            barKg = 20.0,
            hasRack = true,
            hasBench = true,
            hasPullUpBar = false,
            plates = listOf(
                PlatePair(20.0, 4),
                PlatePair(2.5, 0),
                PlatePair(5.0, 4),
            ),
            updatedAt = Instant.now(),
        )
        inventory.smallestPlateKg shouldBe 5.0
    }

    @Test
    fun `smallest plate is null when no plates owned`() {
        val inventory = EquipmentInventory(
            barKg = 20.0,
            hasRack = true,
            hasBench = false,
            hasPullUpBar = false,
            plates = emptyList(),
            updatedAt = Instant.now(),
        )
        inventory.smallestPlateKg.shouldBeNull()
    }
}
