package com.team1.hangsha.auth.dto

data class GoogleLoginRequest(
    val code: String,
    val redirectUri: String? = null
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String
)