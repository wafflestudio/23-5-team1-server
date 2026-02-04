package com.team1.hangsha.user.model

data class AuthTokenPair(
    val accessToken: String,
    val refreshToken: String
)