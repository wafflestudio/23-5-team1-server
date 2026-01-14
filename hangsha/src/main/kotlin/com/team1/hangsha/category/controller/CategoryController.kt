package com.team1.hangsha.category.controller

import com.team1.hangsha.category.dto.CategoryResponse
import com.team1.hangsha.category.dto.core.CategoryDto
import com.team1.hangsha.category.service.CategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Categories")
@RestController
class CategoryController(
    private val categoryService: CategoryService
) {
    @Operation(summary = "카테고리 그룹 + 카테고리 목록")
    @GetMapping("/api/v1/category-groups/with-categories")
    fun getCategoryGroupsWithCategories(): List<CategoryResponse> {
        return categoryService.getCategoryGroupsWithCategories()
    }
    @Operation(summary = "주체기관 카테고리 목록 조회")
    @GetMapping("/categories/orgs")
    fun getOrgCategories(): List<CategoryDto> {
        return categoryService.getOrgCategories()
    }
}
