package com.team1.hangsha.category.repository

import com.team1.hangsha.category.model.Category
import org.springframework.data.repository.CrudRepository

interface CategoryRepository : CrudRepository<Category, Long> {
    fun findByGroupIdAndName(groupId: Long, name: String): Category?
}