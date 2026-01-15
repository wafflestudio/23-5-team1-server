package com.team1.hangsha.category.dto.core

import com.team1.hangsha.category.model.Category

data class CategoryDto(
    val id: Long,
    val groupId: Long,
    val name: String,
    val sortOrder: Int,
) {
    constructor(category: Category) : this(category.id!!, category.groupId, category.name, category.sortOrder)
}