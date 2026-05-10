package com.peter.fitness.domain.model

import java.time.Duration
import java.time.Instant

data class RestTimer(
    val startedAt: Instant,
    val durationSeconds: Long,
    val sessionId: SessionId? = null,
    val setEntryId: SetEntryId? = null,
    val label: String? = null,
) {
    init {
        require(durationSeconds > 0) { "durationSeconds must be positive, was $durationSeconds" }
    }

    val endsAt: Instant get() = startedAt.plusSeconds(durationSeconds)

    fun remainingSeconds(now: Instant): Long =
        Duration.between(now, endsAt).seconds.coerceAtLeast(0L)

    fun isExpired(now: Instant): Boolean = !now.isBefore(endsAt)
}
