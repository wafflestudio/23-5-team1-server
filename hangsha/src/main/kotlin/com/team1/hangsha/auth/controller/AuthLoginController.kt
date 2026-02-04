package com.team1.hangsha.auth.controller

import com.team1.hangsha.auth.dto.SocialLoginRequest
import com.team1.hangsha.auth.dto.TokenResponse
import com.team1.hangsha.auth.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.HttpHeaders

@RestController
@RequestMapping("/api/v1/auth")
class AuthLoginController(
    private val authService: AuthService
) {
    @PostMapping("/login/social")
    fun socialLogin(@RequestBody req: SocialLoginRequest): ResponseEntity<TokenResponse> {
        val result = authService.socialLogin(req)

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, result.refreshCookie.toString())
            .body(TokenResponse(accessToken = result.accessToken, isNewUser = result.isNewUser))
    }
}