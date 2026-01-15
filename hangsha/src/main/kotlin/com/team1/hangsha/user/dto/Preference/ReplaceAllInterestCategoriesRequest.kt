package com.team1.hangsha.user.dto.Preference

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class ReplaceAllInterestCategoriesRequest(
    @field:Valid
    val items: List<Item>
) {
    data class Item(
        @field:NotNull
        val categoryId: Long,

        @field:Min(1)
        val priority: Int
    )
}