package com.team1.hangsha.user.dto.Preference

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class ListExcludedKeywordResponse(
    val items: List<Item>,
) {
    @Schema(name = "ExcludedKeywordItem")
    data class Item(
        val id: Long,
        val keyword: String,
        val createdAt: Instant,
    )
}