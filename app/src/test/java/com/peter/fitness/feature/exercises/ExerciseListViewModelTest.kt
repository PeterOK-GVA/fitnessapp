package com.peter.fitness.feature.exercises

import app.cash.turbine.test
import com.peter.fitness.domain.model.ConditioningSuitability
import com.peter.fitness.domain.model.Exercise
import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.model.LoadType
import com.peter.fitness.domain.model.MovementPattern
import com.peter.fitness.domain.model.TechniqueDemand
import com.peter.fitness.testsupport.FakeExerciseRepository
import com.peter.fitness.testsupport.MainDispatcherExtension
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseListViewModelTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private fun seed(): List<Exercise> = listOf(
        exercise("back-squat", "Back Squat", MovementPattern.SQUAT, TechniqueDemand.HIGH),
        exercise("front-squat", "Front Squat", MovementPattern.SQUAT, TechniqueDemand.HIGH),
        exercise("bench-press", "Bench Press", MovementPattern.HORIZONTAL_PUSH, TechniqueDemand.HIGH),
        exercise("bent-row", "Bent Row", MovementPattern.HORIZONTAL_PULL, TechniqueDemand.MODERATE),
        exercise("conventional-deadlift", "Conventional Deadlift", MovementPattern.HIP_HINGE, TechniqueDemand.HIGH),
    )

    @Test
    fun `loads exercises sorted by name`() = runTest(main.dispatcher) {
        val repo = FakeExerciseRepository(seed())
        val vm = ExerciseListViewModel(repo)

        vm.uiState.test {
            val ready = awaitReady()
            ready.exercises.map { it.name } shouldContainExactly listOf(
                "Back Squat",
                "Bench Press",
                "Bent Row",
                "Conventional Deadlift",
                "Front Squat",
            )
            ready.selectedFilter shouldBe null
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `available filters list each movement pattern present in catalogue exactly once`() =
        runTest(main.dispatcher) {
            val repo = FakeExerciseRepository(seed())
            val vm = ExerciseListViewModel(repo)

            vm.uiState.test {
                val ready = awaitReady()
                ready.availableFilters shouldContainExactlyInAnyOrder listOf(
                    MovementPattern.SQUAT,
                    MovementPattern.HORIZONTAL_PUSH,
                    MovementPattern.HORIZONTAL_PULL,
                    MovementPattern.HIP_HINGE,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `selecting a filter narrows the list to that movement pattern`() = runTest(main.dispatcher) {
        val repo = FakeExerciseRepository(seed())
        val vm = ExerciseListViewModel(repo)

        vm.uiState.test {
            awaitReady()

            vm.onFilterSelected(MovementPattern.SQUAT)
            val filtered = awaitItem()
            filtered.selectedFilter shouldBe MovementPattern.SQUAT
            filtered.exercises.map { it.name } shouldContainExactly listOf("Back Squat", "Front Squat")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearing the filter restores the full list`() = runTest(main.dispatcher) {
        val repo = FakeExerciseRepository(seed())
        val vm = ExerciseListViewModel(repo)

        vm.uiState.test {
            awaitReady()
            vm.onFilterSelected(MovementPattern.SQUAT)
            awaitItem()
            vm.onFilterSelected(null)
            val cleared = awaitItem()
            cleared.selectedFilter shouldBe null
            cleared.exercises.size shouldBe seed().size
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty catalogue surfaces empty list with no filters`() = runTest(main.dispatcher) {
        val repo = FakeExerciseRepository(emptyList())
        val vm = ExerciseListViewModel(repo)

        vm.uiState.test {
            val ready = awaitReady()
            ready.exercises shouldBe emptyList()
            ready.availableFilters shouldBe emptyList()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<ExerciseListUiState>.awaitReady(): ExerciseListUiState {
        var current = awaitItem()
        while (current.isLoading) {
            current = awaitItem()
        }
        return current
    }

    private fun exercise(
        id: String,
        name: String,
        pattern: MovementPattern,
        demand: TechniqueDemand,
    ): Exercise = Exercise(
        id = ExerciseId(id),
        name = name,
        movementPattern = pattern,
        loadType = LoadType.BARBELL,
        techniqueDemand = demand,
        conditioningSuitability = ConditioningSuitability.GOOD,
        createdAt = Instant.EPOCH,
    )
}
