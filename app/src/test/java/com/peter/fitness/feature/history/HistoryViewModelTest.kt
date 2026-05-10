package com.peter.fitness.feature.history

import app.cash.turbine.test
import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.model.Session
import com.peter.fitness.domain.model.SessionFocus
import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.SetEntry
import com.peter.fitness.domain.model.SetEntryId
import com.peter.fitness.testsupport.FakeSessionRepository
import com.peter.fitness.testsupport.MainDispatcherExtension
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val instantA: Instant = Instant.parse("2026-05-08T08:00:00Z")
    private val instantB: Instant = Instant.parse("2026-05-09T17:00:00Z")
    private val instantC: Instant = Instant.parse("2026-05-10T08:00:00Z")

    private fun session(id: String, startedAt: Instant, focus: SessionFocus = SessionFocus.FREE_LOG): Session =
        Session(SessionId(id), startedAt, endedAt = null, focus = focus, notes = null)

    private fun setOf(id: String, sessionId: String): SetEntry = SetEntry(
        id = SetEntryId(id),
        sessionId = SessionId(sessionId),
        exerciseId = ExerciseId("back-squat"),
        ordinal = 0,
        targetReps = 5,
        targetLoadKg = 80.0,
        completedReps = 5,
        performedLoadKg = 80.0,
        subjectiveLoad = null,
        techniqueRating = null,
        createdAt = Instant.EPOCH,
    )

    @Test
    fun `empty repository surfaces empty session list with isLoading false`() = runTest(main.dispatcher) {
        val repo = FakeSessionRepository()
        val vm = HistoryViewModel(repo)

        vm.uiState.test {
            val ready = awaitReady()
            ready.sessions shouldBe emptyList()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sessions are ordered newest-first by startedAt`() = runTest(main.dispatcher) {
        val repo = FakeSessionRepository(
            initialSessions = listOf(
                session("a", instantA),
                session("b", instantB),
                session("c", instantC),
            ),
        )
        val vm = HistoryViewModel(repo)

        vm.uiState.test {
            val ready = awaitReady()
            ready.sessions.map { it.id } shouldContainExactly listOf("c", "b", "a")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `set count is grouped per session`() = runTest(main.dispatcher) {
        val repo = FakeSessionRepository(
            initialSessions = listOf(session("a", instantA), session("b", instantB)),
            initialSets = listOf(
                setOf("s1", "a"),
                setOf("s2", "a"),
                setOf("s3", "a"),
                setOf("s4", "b"),
            ),
        )
        val vm = HistoryViewModel(repo)

        vm.uiState.test {
            val ready = awaitReady()
            val byId = ready.sessions.associateBy { it.id }
            byId["a"]!!.setCount shouldBe 3
            byId["b"]!!.setCount shouldBe 1
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<HistoryUiState>.awaitReady(): HistoryUiState {
        var current = awaitItem()
        while (current.isLoading) {
            current = awaitItem()
        }
        return current
    }
}
