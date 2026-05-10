package com.peter.fitness.domain.usecase

import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.SetEntryId
import com.peter.fitness.testsupport.FakeRestTimerRepository
import com.peter.fitness.testsupport.FakeRestTimerServiceController
import com.peter.fitness.testsupport.MainDispatcherExtension
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class StartRestTimerUseCaseTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val now: Instant = Instant.parse("2026-05-10T08:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `start writes timer to repository and starts service`() = runTest(main.dispatcher) {
        val repo = FakeRestTimerRepository()
        val controller = FakeRestTimerServiceController()
        val useCase = StartRestTimerUseCase(repo, controller, clock)

        useCase(durationSeconds = 90, label = "Rest")

        repo.startCount shouldBe 1
        controller.startCount shouldBe 1
        val active = repo.current()
        active.shouldNotBeNull()
        active.startedAt shouldBe now
        active.durationSeconds shouldBe 90
        active.label shouldBe "Rest"
    }

    @Test
    fun `start preserves session and set entry context when provided`() = runTest(main.dispatcher) {
        val repo = FakeRestTimerRepository()
        val controller = FakeRestTimerServiceController()
        val useCase = StartRestTimerUseCase(repo, controller, clock)

        useCase(
            durationSeconds = 120,
            sessionId = SessionId("session-1"),
            setEntryId = SetEntryId("set-1"),
        )

        val active = repo.current()
        active.shouldNotBeNull()
        active.sessionId shouldBe SessionId("session-1")
        active.setEntryId shouldBe SetEntryId("set-1")
    }

    @Test
    fun `start replaces an in-flight timer`() = runTest(main.dispatcher) {
        val repo = FakeRestTimerRepository()
        val controller = FakeRestTimerServiceController()
        val useCase = StartRestTimerUseCase(repo, controller, clock)

        useCase(durationSeconds = 60)
        useCase(durationSeconds = 120, label = "Different")

        repo.startCount shouldBe 2
        repo.current()?.durationSeconds shouldBe 120
        repo.current()?.label shouldBe "Different"
    }
}
