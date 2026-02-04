package com.team1.hangsha.user.controller

import com.fasterxml.jackson.databind.JsonNode
import com.team1.hangsha.common.upload.LocalUploadService
import com.team1.hangsha.common.upload.dto.UploadResponse
import com.team1.hangsha.user.LoggedInUser
import com.team1.hangsha.user.model.User
import com.team1.hangsha.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.team1.hangsha.user.dto.GetMeResponse
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/users/me")
class UserController(
    private val userService: UserService,
    private val localUploadService: LocalUploadService,
) {
    @GetMapping
    fun getMe(
        @Parameter(hidden = true) @LoggedInUser user: User,
    ): ResponseEntity<GetMeResponse> {
        val me = userService.getMe(user.id!!)
        return ResponseEntity.ok(me)
    }

    @PatchMapping
    fun updateProfile(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @RequestBody body: JsonNode,
    ): ResponseEntity<Void> {
        userService.updateProfile(user.id!!, body)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/profile-image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadProfileImage(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<UploadResponse> {
        val url = localUploadService.uploadProfileImage(user.id!!, file)
        userService.updateProfileImageUrl(user.id!!, url)
        return ResponseEntity.ok(UploadResponse(url = url))
    }
}