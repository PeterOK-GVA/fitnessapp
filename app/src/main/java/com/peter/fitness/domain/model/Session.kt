package com.peter.fitness.domain.model

import java.time.Instant

data class Session(
    val id: SessionId,
    val startedAt: Instant,
    val endedAt: Instant?,
    val focus: SessionFocus,
    val notes: String? = null,
)
