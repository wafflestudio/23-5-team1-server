package com.team1.hangsha.memo.dto.core

import java.time.Instant

data class MemoTagResponse(
    val id: Long,
    val name: String
)

data class MemoResponse(
    val id: Long,
    val eventId: Long,
    val eventTitle: String,
    val content: String,
    val tags: List<String>,
    val createdAt: Instant?,
    val updatedAt: Instant?
)