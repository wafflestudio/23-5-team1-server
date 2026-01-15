package com.team1.hangsha.user.service

import com.team1.hangsha.common.error.DomainException
import com.team1.hangsha.common.error.ErrorCode
import com.team1.hangsha.category.dto.core.CategoryDto
import com.team1.hangsha.category.repository.CategoryRepository
import com.team1.hangsha.user.dto.Preference.ListInterestCategoryResponse
import com.team1.hangsha.user.dto.Preference.ReplaceAllInterestCategoriesRequest
import com.team1.hangsha.user.repository.UserInterestCategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserPreferenceService(
    private val userInterestCategoryRepository: UserInterestCategoryRepository,
    private val categoryRepository: CategoryRepository,
) {

    @Transactional(readOnly = true)
    fun listInterestCategory(userId: Long): List<ListInterestCategoryResponse.Item> {
        val rows = userInterestCategoryRepository.findAllWithCategoryByUserId(userId)

        return rows.map { row ->
            ListInterestCategoryResponse.Item(
                category = CategoryDto(
                    id = row.categoryId,
                    groupId = row.groupId,
                    name = row.name,
                    sortOrder = row.sortOrder,
                ),
                priority = row.priority
            )
        }
    }

    @Transactional
    fun replaceAllInterestCategories(userId: Long, req: ReplaceAllInterestCategoriesRequest) {
        val items = req.items

        // 1) categoryId 중복 금지
        val categoryIds = items.map { it.categoryId }
        if (categoryIds.size != categoryIds.distinct().size) {
            throw DomainException(ErrorCode.INVALID_REQUEST)
        }

        // 2) priority 중복 금지
        val priorities = items.map { it.priority }
        if (priorities.size != priorities.distinct().size) {
            throw DomainException(ErrorCode.INVALID_REQUEST)
        }

        // 3) priority 연속(1..N) 강제 (권장)
        if (items.isNotEmpty()) {
            val sorted = priorities.sorted()
            val expected = (1..items.size).toList()
            if (sorted != expected) {
                throw DomainException(ErrorCode.INVALID_REQUEST)
            }
        }

        // 4) category 존재 검증 (IN 한번)
        if (items.isNotEmpty()) {
            val existCount = categoryRepository.countByIds(categoryIds)
            if (existCount != categoryIds.size) {
                throw DomainException(ErrorCode.INVALID_REQUEST)
            }
        }

        // 5) 전체 교체 (트랜잭션)
        userInterestCategoryRepository.deleteAllByUserId(userId)

        if (items.isNotEmpty()) {
            userInterestCategoryRepository.saveAll(
                items.map {
                    com.team1.hangsha.user.model.UserInterestCategory(
                        userId = userId,
                        categoryId = it.categoryId,
                        priority = it.priority
                    )
                }
            )
        }
    }

    @Transactional
    fun delete(userId: Long, categoryId: Long) {
        val affected = userInterestCategoryRepository.deleteByUserIdAndCategoryId(userId, categoryId)
        if (affected == 0) {
            throw DomainException(ErrorCode.INVALID_REQUEST)
        }
    }
}