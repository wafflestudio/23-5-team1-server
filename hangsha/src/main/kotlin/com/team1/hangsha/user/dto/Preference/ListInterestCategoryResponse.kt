package com.team1.hangsha.user.dto.Preference

import com.team1.hangsha.category.dto.core.CategoryDto

data class ListInterestCategoryResponse(
    val items: List<Item>
) {
    data class Item(
        val category: CategoryDto,
        val priority: Int
    )
}