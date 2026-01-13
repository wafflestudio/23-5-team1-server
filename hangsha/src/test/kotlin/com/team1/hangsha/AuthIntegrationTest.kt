package com.team1.hangsha

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    properties = [
        "jwt.secret=v3rys3cr3tk3y_must_be_l0ng_enough_to_be_secure_minimum_256_bits__test",
        "jwt.access-expiration-ms=3600000",
        "jwt.refresh-expiration-ms=1209600000",
    ],
)
class AuthIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    data class RegisterRequest(val email: String, val password: String)
    data class LoginRequest(val email: String, val password: String)

    data class TokenPairResponse(val accessToken: String, val refreshToken: String)
    data class RefreshResponse(val accessToken: String)

    @Test
    fun `register login refresh flow works`() {
        val email = "test_${UUID.randomUUID()}@example.com"
        val password = "Abcd1234!" // 정책: 8자+, 영문/숫자/특수문자 포함, 공백 없음

        // 1) 회원가입 (회원가입 응답이 토큰을 준다는 문서 스펙 기준)
        val registerResult = mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest(email, password))
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.accessToken") { exists() }
            jsonPath("$.refreshToken") { exists() }
        }.andReturn()

        val registerBody = registerResult.response.contentAsString
        val registerTokens = objectMapper.readValue(registerBody, TokenPairResponse::class.java)

        // 2) 로그인
        val loginResult = mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest(email, password))
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.accessToken") { exists() }
            jsonPath("$.refreshToken") { exists() }
        }.andReturn()

        val loginBody = loginResult.response.contentAsString
        val loginTokens = objectMapper.readValue(loginBody, TokenPairResponse::class.java)

        // 로그인으로 발급된 accessToken은 register에서 받은 것과 달라도 정상(대부분 다름)
        assertNotEquals(registerTokens.accessToken, loginTokens.accessToken)

        // 3) refresh (Authorization 헤더에 refreshToken을 Bearer로 넣는 스펙)
        val refreshResult = mockMvc.post("/api/v1/auth/refresh") {
            header("Authorization", "Bearer ${loginTokens.refreshToken}")
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.accessToken") { exists() }
        }.andReturn()

        val refreshBody = refreshResult.response.contentAsString
        val refreshed = objectMapper.readValue(refreshBody, RefreshResponse::class.java)

        // refresh로 새 accessToken이 발급되는지(보통 기존과 다름)
        assertNotEquals(loginTokens.accessToken, refreshed.accessToken)
    }

    @Test
    fun `login fails with wrong password`() {
        val email = "test_${UUID.randomUUID()}@example.com"
        val password = "Abcd1234!"
        val wrongPassword = "Abcd1234?wrong"

        // register
        mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest(email, password))
        }.andExpect {
            status { isOk() }
        }

        // login with wrong password -> 401 기대 (너 DomainException 매핑이 401이도록 되어있다면)
        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest(email, wrongPassword))
        }.andExpect {
            status { isUnauthorized() } // 만약 너희 핸들러가 400/404로 주면 여기만 맞춰 바꾸면 됨
        }
    }

    @Test
    fun `refresh fails with access token`() {
        val email = "test_${UUID.randomUUID()}@example.com"
        val password = "Abcd1234!"

        val loginResult = mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest(email, password))
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val tokens = objectMapper.readValue(loginResult.response.contentAsString, TokenPairResponse::class.java)

        // refresh endpoint에 accessToken을 넣으면 validateRefreshToken에서 걸려야 함
        mockMvc.post("/api/v1/auth/refresh") {
            header("Authorization", "Bearer ${tokens.accessToken}")
        }.andExpect {
            status { isUnauthorized() } // AUTH_INVALID_TOKEN이 401이라면
        }
    }
}