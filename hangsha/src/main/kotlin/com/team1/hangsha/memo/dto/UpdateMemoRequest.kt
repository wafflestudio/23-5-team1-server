package com.team1.hangsha.memo.dto

data class UpdateMemoRequest(
    val content: String,
    val tagNames: List<String>
)