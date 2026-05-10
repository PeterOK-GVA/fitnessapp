package com.peter.fitness.domain.usecase

import com.peter.fitness.domain.model.RestTimer
import com.peter.fitness.testsupport.FakeRestTimerRepository
import com.peter.fitness.testsupport.FakeRestTimerServiceController
import com.peter.fitness.testsupport.MainDispatcherExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CancelRestTimerUseCaseTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    @Test
    fun `cancel clears repository and stops service`() = runTest(main.dispatcher) {
        val active = RestTimer(
            startedAt = Instant.parse("2026-05-10T08:00:00Z"),
            durationSeconds = 90,
        )
        val repo = FakeRestTimerRepository(initial = active)
        val controller = FakeRestTimerServiceController()
        val useCase = CancelRestTimerUseCase(repo, controller)

        useCase()

        repo.current().shouldBeNull()
        repo.cancelCount shouldBe 1
        controller.stopCount shouldBe 1
    }

    @Test
    fun `cancel is safe when no timer is active`() = runTest(main.dispatcher) {
        val repo = FakeRestTimerRepository()
        val controller = FakeRestTimerServiceController()
        val useCase = CancelRestTimerUseCase(repo, controller)

        useCase()

        repo.cancelCount shouldBe 1
        controller.stopCount shouldBe 1
    }
}
