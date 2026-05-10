package com.peter.fitness.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class RestTimerTest {

    private val start: Instant = Instant.parse("2026-05-10T08:00:00Z")

    @Test
    fun `rejects zero duration`() {
        shouldThrow<IllegalArgumentException> {
            RestTimer(startedAt = start, durationSeconds = 0)
        }
    }

    @Test
    fun `rejects negative duration`() {
        shouldThrow<IllegalArgumentException> {
            RestTimer(startedAt = start, durationSeconds = -1)
        }
    }

    @Test
    fun `endsAt is startedAt + durationSeconds`() {
        val timer = RestTimer(startedAt = start, durationSeconds = 90)
        timer.endsAt shouldBe start.plusSeconds(90)
    }

    @Test
    fun `remainingSeconds counts down to zero`() {
        val timer = RestTimer(startedAt = start, durationSeconds = 60)
        timer.remainingSeconds(start) shouldBe 60
        timer.remainingSeconds(start.plusSeconds(15)) shouldBe 45
        timer.remainingSeconds(start.plusSeconds(60)) shouldBe 0
    }

    @Test
    fun `remainingSeconds clamps at zero past expiry`() {
        val timer = RestTimer(startedAt = start, durationSeconds = 60)
        timer.remainingSeconds(start.plusSeconds(120)) shouldBe 0
    }

    @Test
    fun `isExpired flips at endsAt`() {
        val timer = RestTimer(startedAt = start, durationSeconds = 60)
        timer.isExpired(start.plusSeconds(59)) shouldBe false
        timer.isExpired(start.plusSeconds(60)) shouldBe true
        timer.isExpired(start.plusSeconds(120)) shouldBe true
    }
}
