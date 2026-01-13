package com.team1.hangsha.user.dto

import com.team1.hangsha.user.model.AuthProvider

data class SocialRegisterRequest(
    val provider: AuthProvider,
    val idToken: String,
    val email: String? = null,    // provider가 email을 안 주는 경우 고려
)