package com.team1.hangsha.auth.dto

import org.springframework.http.ResponseCookie

data class SocialLoginResult(
    val accessToken: String,
    val refreshCookie: ResponseCookie,
    val isNewUser: Boolean
)