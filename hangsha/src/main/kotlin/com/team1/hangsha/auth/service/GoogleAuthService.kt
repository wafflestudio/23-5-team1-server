package com.team1.hangsha.auth.service

import com.fasterxml.jackson.databind.JsonNode
import com.team1.hangsha.auth.dto.LoginResponse
import com.team1.hangsha.user.JwtTokenProvider
import com.team1.hangsha.user.model.User
import com.team1.hangsha.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

@Service
class GoogleAuthService(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val restTemplate: RestTemplate = RestTemplate() // Config에 Bean 등록 안했으면 여기서 생성
) {

    @Value("\${spring.security.oauth2.client.registration.google.client-id}")
    private lateinit var clientId: String

    @Value("\${spring.security.oauth2.client.registration.google.client-secret}")
    private lateinit var clientSecret: String

    @Transactional
    fun googleLogin(code: String, redirectUriFromFront: String?): LoginResponse {
        // 1. 프론트에서 받은 Code로 구글 Access Token 획득
        val googleAccessToken = getGoogleAccessToken(code, redirectUriFromFront)

        // 2. 구글 Access Token으로 유저 정보 조회
        val googleUserNode = getGoogleUserInfo(googleAccessToken)

        val email = googleUserNode.get("email").asText()
        val name = googleUserNode.get("name").asText()
        val picture = googleUserNode.get("picture")?.asText() // 프로필 이미지

        // 3. DB 조회 및 회원가입/로그인 처리
        // 이메일로 기존 유저 확인, 없으면 새로 저장
        val user = userRepository.findByEmail(email)
            ?: userRepository.save(
                User(
                    email = email,
                    username = name,
                    profileImageUrl = picture
                )
            )

        // 4. 기존 JwtTokenProvider를 사용하여 우리 서비스의 토큰 발급
        // user.id!!는 save 후에는 반드시 존재함
        val accessToken = jwtTokenProvider.createAccessToken(user.id!!)
        val refreshToken = jwtTokenProvider.createRefreshToken(user.id!!)

        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    private fun getGoogleAccessToken(code: String, redirectUriFromFront: String?): String {
        val tokenUrl = "https://oauth2.googleapis.com/token"

        val params = LinkedMultiValueMap<String, String>()
        params.add("code", code)
        params.add("client_id", clientId)
        params.add("client_secret", clientSecret)
        // 프론트엔드가 'auth-code' 방식(팝업) 사용 시 'postmessage' 전송됨
        params.add("redirect_uri", redirectUriFromFront ?: "postmessage")
        params.add("grant_type", "authorization_code")

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val requestEntity = HttpEntity(params, headers)

        val response = restTemplate.postForEntity(tokenUrl, requestEntity, JsonNode::class.java)

        if (response.statusCode == HttpStatus.OK) {
            return response.body?.get("access_token")?.asText()
                ?: throw RuntimeException("Google Access Token is null")
        }
        throw RuntimeException("Google Login Failed: ${response.statusCode}")
    }

    private fun getGoogleUserInfo(accessToken: String): JsonNode {
        val userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo"

        val headers = HttpHeaders()
        headers.setBearerAuth(accessToken)

        val requestEntity = HttpEntity<String>(headers)

        val response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, requestEntity, JsonNode::class.java)

        if (response.statusCode == HttpStatus.OK) {
            return response.body ?: throw RuntimeException("User Info is null")
        }
        throw RuntimeException("Failed to get User Info")
    }
}