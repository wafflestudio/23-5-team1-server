package com.team1.hangsha.user.dto.Preference

import com.team1.hangsha.category.dto.core.CategoryDto
import io.swagger.v3.oas.annotations.media.Schema

data class ListInterestCategoryResponse(
    val items: List<Item>
) {
    @Schema(name = "InterestCategoryItem")
    data class Item(
        val category: CategoryDto,
        val priority: Int
    )
}