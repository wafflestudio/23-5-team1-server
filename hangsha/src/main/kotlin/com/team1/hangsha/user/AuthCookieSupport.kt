package com.team1.hangsha.user

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component

@Component
class AuthCookieSupport(
    @Value("\${auth.refresh-cookie.secure}")
    private val secure: Boolean,
    @Value("\${auth.refresh-cookie.same-site}")
    private val sameSite: String,
) {
    fun buildRefreshCookie(token: String, maxAgeSeconds: Long): ResponseCookie =
        ResponseCookie.from("refreshToken", token)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/api/v1/auth")
            .maxAge(maxAgeSeconds)
            .build()

    fun clearRefreshCookie(): ResponseCookie =
        ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/api/v1/auth")
            .maxAge(0)
            .build()
}