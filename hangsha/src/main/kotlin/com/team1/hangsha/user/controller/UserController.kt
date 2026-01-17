package com.team1.hangsha.user.controller

import com.fasterxml.jackson.databind.JsonNode
import com.team1.hangsha.user.LoggedInUser
import com.team1.hangsha.user.model.User
import com.team1.hangsha.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.team1.hangsha.user.dto.GetMeResponse

@RestController
@RequestMapping("/api/v1/users/me")
class UserController(
    private val userService: UserService,
) {
    @GetMapping
    fun getMe(
        @LoggedInUser user: User,
    ): ResponseEntity<GetMeResponse> {
        val me = userService.getMe(user.id!!)
        return ResponseEntity.ok(me)
    }

    @PatchMapping
    fun updateProfile(
        @LoggedInUser user: User,
        @RequestBody body: JsonNode,
    ): ResponseEntity<Void> {
        userService.updateProfile(user.id!!, body)
        return ResponseEntity.noContent().build()
    }
}