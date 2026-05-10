package com.peter.fitness.feature.home

import app.cash.turbine.test
import com.peter.fitness.core.ids.IdFactory
import com.peter.fitness.domain.model.SessionFocus
import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.SetEntryId
import com.peter.fitness.testsupport.FakeSessionRepository
import com.peter.fitness.testsupport.MainDispatcherExtension
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val fixedInstant: Instant = Instant.parse("2026-05-10T08:00:00Z")
    private val fixedClock: Clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

    @Test
    fun `onStartWorkout creates a session and emits SessionStarted with the new id`() =
        runTest(main.dispatcher) {
            val repo = FakeSessionRepository()
            val vm = HomeViewModel(
                sessionRepository = repo,
                clock = fixedClock,
                idFactory = FixedIdFactory(SessionId("new-session-id")),
            )

            vm.events.test {
                vm.onStartWorkout()
                val event = awaitItem()
                event shouldBe HomeEvent.SessionStarted("new-session-id")
                cancelAndIgnoreRemainingEvents()
            }

            repo.createCount shouldBe 1
            val created = repo.createdSessions.single()
            created.id shouldBe SessionId("new-session-id")
            created.startedAt shouldBe fixedInstant
            created.endedAt shouldBe null
            created.focus shouldBe SessionFocus.FREE_LOG
        }

    private class FixedIdFactory(private val sessionId: SessionId) : IdFactory {
        override fun newSessionId(): SessionId = sessionId
        override fun newSetEntryId(): SetEntryId = SetEntryId("unused")
    }
}
