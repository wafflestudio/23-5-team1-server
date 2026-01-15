package com.team1.hangsha.user.repository

import com.team1.hangsha.user.model.UserInterestCategory
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface UserInterestCategoryRepository : CrudRepository<UserInterestCategory, Long> {

    @Query(
        """
        select
          c.id as category_id,
          c.group_id as group_id,
          c.name as name,
          c.sort_order as sort_order,
          uic.priority as priority
        from user_interest_categories uic
        join categories c on c.id = uic.category_id
        where uic.user_id = :userId
        order by uic.priority asc
        """
    )
    fun findAllWithCategoryByUserId(
        @Param("userId") userId: Long
    ): List<InterestCategoryRow>

    @Modifying
    @Query(
        """
        delete from user_interest_categories
        where user_id = :userId
        """
    )
    fun deleteAllByUserId(
        @Param("userId") userId: Long
    ): Int

    @Modifying
    @Query(
        """
        delete from user_interest_categories
        where user_id = :userId and category_id = :categoryId
        """
    )
    fun deleteByUserIdAndCategoryId(
        @Param("userId") userId: Long,
        @Param("categoryId") categoryId: Long
    ): Int

    data class InterestCategoryRow(
        val categoryId: Long,
        val groupId: Long,
        val name: String,
        val sortOrder: Int,
        val priority: Int,
    )
}