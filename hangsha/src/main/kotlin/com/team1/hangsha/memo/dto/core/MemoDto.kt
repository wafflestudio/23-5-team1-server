package com.team1.hangsha.memo.dto.core

import java.time.Instant

data class CreateMemoRequest(
    val eventId: Long,
    val content: String,
    val tagNames: List<String> = emptyList()
)

data class MemoResponse(
    val id: Long,
    val eventId: Long,
    val eventTitle: String,
    val content: String,
    val tags: List<String>,
    val createdAt: Instant?
)

data class TagResponse(
    val id: Long,
    val name: String
)