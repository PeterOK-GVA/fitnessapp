package com.peter.fitness.feature.equipment

import com.peter.fitness.domain.model.EquipmentInventory
import com.peter.fitness.domain.model.PlatePair
import com.peter.fitness.testsupport.FakeEquipmentInventoryRepository
import com.peter.fitness.testsupport.MainDispatcherExtension
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class EquipmentViewModelTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val seed = EquipmentInventory(
        barKg = 20.0,
        hasRack = true,
        hasBench = true,
        hasPullUpBar = false,
        plates = listOf(
            PlatePair(20.0, 4),
            PlatePair(10.0, 4),
            PlatePair(2.5, 4),
        ),
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun `init loads current inventory into ui state`() = runTest(main.dispatcher) {
        val repo = FakeEquipmentInventoryRepository(seed)
        val vm = EquipmentViewModel(repo)
        advanceUntilIdle()

        val state = vm.uiState.value
        state.isLoading shouldBe false
        state.barKg shouldBe "20"
        state.hasRack shouldBe true
        state.hasBench shouldBe true
        state.hasPullUpBar shouldBe false
        state.plates.map { it.denominationKg to it.pairCount } shouldContainExactlyInAnyOrder listOf(
            "20" to "4",
            "10" to "4",
            "2.5" to "4",
        )
        state.isDirty shouldBe false
    }

    @Test
    fun `editing bar weight marks state dirty`() = runTest(main.dispatcher) {
        val repo = FakeEquipmentInventoryRepository(seed)
        val vm = EquipmentViewModel(repo)
        advanceUntilIdle()

        vm.onBarKgChange("15")

        vm.uiState.value.barKg shouldBe "15"
        vm.uiState.value.isDirty shouldBe true
    }

    @Test
    fun `save with invalid bar weight surfaces error and does not persist`() = runTest(main.dispatcher) {
        val repo = FakeEquipmentInventoryRepository(seed)
        val vm = EquipmentViewModel(repo)
        advanceUntilIdle()

        vm.onBarKgChange("not a number")
        vm.onSave()
        advanceUntilIdle()

        vm.uiState.value.barKgError.shouldNotBeNull()
        repo.updateCount shouldBe 0
    }

    @Test
    fun `save with negative bar weight surfaces error`() = runTest(main.dispatcher) {
        val repo = FakeEquipmentInventoryRepository(seed)
        val vm = EquipmentViewModel(repo)
        advanceUntilIdle()

        vm.onBarKgChange("-5")
        vm.onSave()
        advanceUntilIdle()

        vm.uiState.value.barKgError.shouldNotBeNull()
        repo.updateCount shouldBe 0
    }

    @Test
    fun `save with non-positive plate denomination surfaces error`() = runTest(main.dispatcher) {
        val repo = FakeEquipmentInventoryRepository(seed)
        val vm = EquipmentViewModel(repo)
        advanceUntilIdle()

        val firstKey = vm.uiState.value.plates.first().key
        vm.onPlateDenominationChange(firstKey, "0")
        vm.onSave()
        advanceUntilIdle()

        vm.uiState.value.plateErrors[firstKey].shouldNotBeNull()
        repo.updateCount shouldBe 0
    }

    @Test
    fun `valid save persists and clears dirty flag`() = runTest(main.dispatcher) {
        val repo = FakeEquipmentInventoryRepository(seed)
        val vm = EquipmentViewModel(repo)
        advanceUntilIdle()

        vm.onBarKgChange("15")
        vm.onHasPullUpBarChange(true)
        vm.onSave()
        advanceUntilIdle()

        repo.updateCount shouldBe 1
        repo.current().barKg shouldBe 15.0
        repo.current().hasPullUpBar shouldBe true
        vm.uiState.value.isDirty shouldBe false
        vm.uiState.value.justSaved shouldBe true
        vm.uiState.value.barKgError.shouldBeNull()
    }

    @Test
    fun `add plate appends a row and remove drops it`() = runTest(main.dispatcher) {
        val repo = FakeEquipmentInventoryRepository(seed)
        val vm = EquipmentViewModel(repo)
        advanceUntilIdle()

        val sizeBefore = vm.uiState.value.plates.size

        vm.onAddPlate()
        vm.uiState.value.plates.size shouldBe sizeBefore + 1

        val newKey = vm.uiState.value.plates.last().key
        vm.onPlateDenominationChange(newKey, "1.25")
        vm.onPlateCountChange(newKey, "2")
        vm.onSave()
        advanceUntilIdle()

        repo.current().plates.any { it.denominationKg == 1.25 && it.pairCount == 2 } shouldBe true

        vm.onRemovePlate(newKey)
        vm.uiState.value.plates.any { it.key == newKey } shouldBe false
    }

    @Test
    fun `valid save uses parsed bar weight even with whitespace`() = runTest(main.dispatcher) {
        val repo = FakeEquipmentInventoryRepository(seed)
        val vm = EquipmentViewModel(repo)
        advanceUntilIdle()

        vm.onBarKgChange("  15  ")
        vm.onSave()
        advanceUntilIdle()

        repo.updateCount shouldBe 1
        repo.current().barKg shouldBe 15.0
    }
}
