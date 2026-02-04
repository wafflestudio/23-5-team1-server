package com.team1.hangsha.user.controller

import com.team1.hangsha.user.dto.LoginRequest
import com.team1.hangsha.user.dto.LoginResponse
import com.team1.hangsha.user.dto.RegisterRequest
import com.team1.hangsha.user.dto.RegisterResponse
import com.team1.hangsha.user.dto.RefreshResponse
import com.team1.hangsha.user.service.UserService
import com.team1.hangsha.common.error.ErrorCode
import com.team1.hangsha.common.error.DomainException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestHeader

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val userService: UserService,
) {
    @PostMapping("/register")
    fun register(
        @RequestBody registerRequest: RegisterRequest,
    ): ResponseEntity<RegisterResponse> {
        val userDto = userService.localRegister(
            email = registerRequest.email,
            password = registerRequest.password,
        )

        val tokenPair = userService.localLogin(
            email = registerRequest.email,
            password = registerRequest.password,
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                RegisterResponse(
                    accessToken = tokenPair.accessToken,
                    refreshToken = tokenPair.refreshToken,
                )
            )
    }

    @PostMapping("/login")
    fun login(
        @RequestBody loginRequest: LoginRequest,
    ): ResponseEntity<LoginResponse> {
        val tokenPair = userService.localLogin(
            email = loginRequest.email,
            password = loginRequest.password,
        )

        return ResponseEntity
            .ok(
                LoginResponse(
                    accessToken = tokenPair.accessToken,
                    refreshToken = tokenPair.refreshToken,
                )
            )
    }

    @PostMapping("/refresh")
    fun refresh(@RequestHeader("Authorization", required = false) authorization: String?)
            : ResponseEntity<RefreshResponse> {

        val refreshToken = extractBearerToken(authorization)
            ?: throw DomainException(ErrorCode.AUTH_UNAUTHORIZED)

        val accessToken = userService.refreshAccessToken(refreshToken)
        return ResponseEntity.ok(RefreshResponse(accessToken))
    }

    private fun extractBearerToken(authorization: String?): String? {
        if (authorization.isNullOrBlank()) return null
        if (!authorization.startsWith("Bearer ")) return null
        return authorization.substring(7).trim().ifBlank { null }
    }
}