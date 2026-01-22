package com.team1.hangsha.user.controller

import com.team1.hangsha.user.LoggedInUser
import com.team1.hangsha.user.model.User
import com.team1.hangsha.user.service.UserPreferenceService
import com.team1.hangsha.user.dto.Preference.ListInterestCategoryResponse
import com.team1.hangsha.user.dto.Preference.ReplaceAllInterestCategoriesRequest
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PathVariable


@RestController
@RequestMapping("/api/v1/users/me")
class UserPreferenceController (
    private val userPreferenceService: UserPreferenceService,
) {
    @GetMapping("/interest-categories")
    fun listInterestCategory(
        @Parameter(hidden = true) @LoggedInUser user: User,
        ): ResponseEntity<ListInterestCategoryResponse> {

        val items = userPreferenceService.listInterestCategory(user.id!!)
        return ResponseEntity.ok(ListInterestCategoryResponse(items))
    }

    @PutMapping("/interest-categories")
    fun replaceAllInterestCategories(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @Valid  @RequestBody req: ReplaceAllInterestCategoriesRequest,
    ): ResponseEntity<Void> {
        userPreferenceService.replaceAllInterestCategories(user.id!!, req)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/interest-categories/{categoryId}")
    fun deleteInterestCategory(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable categoryId: Long
    ): ResponseEntity<Void> {
        userPreferenceService.delete(user.id!!, categoryId)
        return ResponseEntity.noContent().build()
    }

}