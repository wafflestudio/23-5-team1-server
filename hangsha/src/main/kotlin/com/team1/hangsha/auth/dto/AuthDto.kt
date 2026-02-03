package com.team1.hangsha.auth.dto

data class SocialLoginRequest(
    val provider: String,
    val code: String,
    val codeVerifier: String?
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)