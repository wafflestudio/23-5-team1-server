package com.team1.hangsha.memo.dto

import java.time.Instant

data class UpdateMemoResponse(
    val id: Long,
    val eventId: Long,
    val eventTitle: String,
    val content: String,
    val tags: List<String>,
    val updatedAt: Instant?
)