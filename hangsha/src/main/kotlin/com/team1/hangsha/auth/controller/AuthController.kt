package com.team1.hangsha.auth.controller

import com.team1.hangsha.auth.dto.GoogleLoginRequest
import com.team1.hangsha.auth.dto.LoginResponse
import com.team1.hangsha.auth.service.GoogleAuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val googleAuthService: GoogleAuthService
) {

    @PostMapping("/login/google")
    fun googleLogin(@RequestBody request: GoogleLoginRequest): ResponseEntity<LoginResponse> {
        val loginResponse = googleAuthService.googleLogin(request.code, request.redirectUri)
        return ResponseEntity.ok(loginResponse)
    }
}