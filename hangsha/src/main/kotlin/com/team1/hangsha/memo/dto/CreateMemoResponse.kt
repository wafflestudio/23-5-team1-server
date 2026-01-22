package com.team1.hangsha.memo.dto

import java.time.Instant

data class CreateMemoResponse(
    val id: Long,
    val eventId: Long,
    val eventTitle: String,
    val content: String,
    val tags: List<String>,
    val createdAt: Instant?
)