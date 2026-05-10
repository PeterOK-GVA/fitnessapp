package com.peter.fitness.domain.plates

import com.peter.fitness.domain.model.EquipmentInventory
import com.peter.fitness.domain.model.PlatePair
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.math.abs

class PlateCalculatorTest {

    private val standardInventory = EquipmentInventory(
        barKg = 20.0,
        hasRack = true,
        hasBench = true,
        hasPullUpBar = false,
        plates = listOf(
            PlatePair(20.0, 4),
            PlatePair(15.0, 2),
            PlatePair(10.0, 4),
            PlatePair(5.0, 4),
            PlatePair(2.5, 4),
            PlatePair(1.25, 2),
        ),
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun `target equal to bar weight returns no plates`() {
        val result = PlateCalculator.snap(20.0, standardInventory)
        result.achievableKg shouldBe 20.0
        result.perSide.shouldBeEmpty()
        result.barKg shouldBe 20.0
    }

    @Test
    fun `target less than bar weight returns just bar`() {
        val result = PlateCalculator.snap(10.0, standardInventory)
        result.achievableKg shouldBe 20.0
        result.perSide.shouldBeEmpty()
    }

    @Test
    fun `target of 100kg uses two 20kg plates per side`() {
        val result = PlateCalculator.snap(100.0, standardInventory)
        result.achievableKg shouldBe 100.0
        result.perSide shouldBe listOf(PlateLoad(20.0, 2))
    }

    @Test
    fun `target of 87 point 5kg achievable exactly`() {
        // 33.75 per side: 20 + 10 + 2.5 + 1.25
        val result = PlateCalculator.snap(87.5, standardInventory)
        result.achievableKg shouldBe 87.5
        result.totalPlatesKg shouldBe 67.5
    }

    @Test
    fun `unreachable target rounds to closest achievable`() {
        val coarse = standardInventory.copy(
            plates = listOf(
                PlatePair(20.0, 4),
                PlatePair(10.0, 4),
                PlatePair(5.0, 4),
                PlatePair(2.5, 4),
            ),
        )
        // Target 102.5; per-side 41.25. Coarsest plate 2.5 -> per-side achievable in 2.5 increments.
        // Closest achievable per-side is either 40.0 or 42.5; tiebreak picks 40 -> total 100.
        val result = PlateCalculator.snap(102.5, coarse)
        result.achievableKg shouldBe 100.0
    }

    @Test
    fun `target exceeding inventory caps at maximum achievable`() {
        val tiny = EquipmentInventory(
            barKg = 20.0,
            hasRack = true,
            hasBench = false,
            hasPullUpBar = false,
            plates = listOf(PlatePair(20.0, 1)), // one pair of 20 -> max per side 20
            updatedAt = Instant.EPOCH,
        )
        val result = PlateCalculator.snap(500.0, tiny)
        result.achievableKg shouldBe 60.0 // 20 bar + 2 * 20
        result.perSide shouldBe listOf(PlateLoad(20.0, 1))
    }

    @Test
    fun `inventory with no plates returns just bar regardless of target`() {
        val noPlates = EquipmentInventory(
            barKg = 20.0,
            hasRack = true,
            hasBench = true,
            hasPullUpBar = false,
            plates = emptyList(),
            updatedAt = Instant.EPOCH,
        )
        PlateCalculator.snap(0.0, noPlates).achievableKg shouldBe 20.0
        PlateCalculator.snap(20.0, noPlates).achievableKg shouldBe 20.0
        PlateCalculator.snap(100.0, noPlates).achievableKg shouldBe 20.0
        PlateCalculator.snap(100.0, noPlates).perSide.shouldBeEmpty()
    }

    @Test
    fun `zero pairCount plates are ignored`() {
        val mixed = standardInventory.copy(
            plates = listOf(
                PlatePair(25.0, 0), // pretend we sold these
                PlatePair(20.0, 4),
                PlatePair(2.5, 0),
                PlatePair(1.25, 2),
            ),
        )
        val result = PlateCalculator.snap(102.5, mixed)
        // No 25 or 2.5 plates available
        result.perSide.none { it.denominationKg == 25.0 } shouldBe true
        result.perSide.none { it.denominationKg == 2.5 } shouldBe true
    }

    @Test
    fun `property - achievable load equals bar plus 2x stack contribution`() = runTest {
        checkAll(arbInventory, arbTarget) { inventory, target ->
            val result = PlateCalculator.snap(target, inventory)
            val computed = result.barKg + 2.0 * result.perSide.sumOf { it.denominationKg * it.countPerSide }
            computed shouldBe (result.achievableKg plusOrMinus EPSILON)
        }
    }

    @Test
    fun `property - never uses more plates per side than inventory has pairs`() = runTest {
        checkAll(arbInventory, arbTarget) { inventory, target ->
            val result = PlateCalculator.snap(target, inventory)
            result.perSide.forEach { stack ->
                val available =
                    inventory.plates.firstOrNull { it.denominationKg == stack.denominationKg }?.pairCount ?: 0
                stack.countPerSide shouldBeLessThanOrEqualTo available
            }
        }
    }

    @Test
    fun `property - result is closest achievable load to target (ties broken low)`() = runTest {
        checkAll(arbInventory, arbTarget) { inventory, target ->
            val result = PlateCalculator.snap(target, inventory)
            val achievableLoads = bruteForceAchievableTotals(inventory)
            val minDist = achievableLoads.minOf { abs(it - target) }
            abs(result.achievableKg - target) shouldBe (minDist plusOrMinus EPSILON)
        }
    }

    @Test
    fun `property - calling snap twice gives the same result`() = runTest {
        checkAll(arbInventory, arbTarget) { inventory, target ->
            PlateCalculator.snap(target, inventory) shouldBe PlateCalculator.snap(target, inventory)
        }
    }

    companion object {
        const val EPSILON = 0.0001

        val arbDenomination: Arb<Double> = Arb.element(1.25, 2.5, 5.0, 10.0, 15.0, 20.0, 25.0)

        val arbBar: Arb<Double> = Arb.element(15.0, 20.0)

        val arbInventory: Arb<EquipmentInventory> = arbitrary {
            val rawPlates = Arb.list(
                arbitrary { PlatePair(arbDenomination.bind(), Arb.int(1..5).bind()) },
                range = 1..6,
            ).bind()
            val plates = rawPlates.distinctBy { it.denominationKg }
            EquipmentInventory(
                barKg = arbBar.bind(),
                hasRack = Arb.boolean().bind(),
                hasBench = Arb.boolean().bind(),
                hasPullUpBar = Arb.boolean().bind(),
                plates = plates,
                updatedAt = Instant.EPOCH,
            )
        }

        val arbTarget: Arb<Double> = Arb.double(min = 0.0, max = 300.0)

        // Independent enumeration of achievable total loads, used to verify "closest" property.
        fun bruteForceAchievableTotals(inv: EquipmentInventory): Set<Double> {
            var perSideStates = setOf(0.0)
            for ((denom, count) in inv.plates.filter { it.pairCount > 0 }) {
                perSideStates = perSideStates.flatMap { existing ->
                    (0..count).map { n -> existing + n * denom }
                }.toSet()
            }
            return perSideStates.map { inv.barKg + 2.0 * it }.toSet()
        }
    }
}
