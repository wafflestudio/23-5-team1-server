package com.team1.hangsha.category.repository

import com.team1.hangsha.category.model.Category
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.data.jdbc.repository.query.Query

interface CategoryRepository : CrudRepository<Category, Long> {
    fun findByGroupIdAndName(groupId: Long, name: String): Category?

    @Query(
        """
        select count(*) from categories
        where id in (:ids)
        """
    )
    fun countByIds(@Param("ids") ids: List<Long>): Int
}