package com.team1.hangsha.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class SocialLoginRequest(
    val provider: String,
    val code: String? = null, // web 필수, mobile null
    val accessToken: String? = null, // web null, mobile 필수
    val identityToken: String? = null, // Apple native identity JWT
    val userIdentifier: String? = null, // Apple native credential.user (verified against JWT sub)
    val email: String? = null, // Apple가 최초 승인에서만 제공할 수 있는 참고값
    val name: String? = null, // Apple가 최초 승인에서만 제공할 수 있는 이름
    val codeVerifier: String? = null, // 구글 PKCE용
    @JsonProperty("client_type")
    val clientType: String? = "WEB"
)

data class TokenResponse(
    val accessToken: String,
    val isNewUser: Boolean
)
