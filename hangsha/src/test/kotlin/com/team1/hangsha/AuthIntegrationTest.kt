package com.team1.hangsha

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
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
    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    data class RegisterRequest(val email: String, val password: String)
    data class LoginRequest(val email: String, val password: String)

    data class TokenPairResponse(val accessToken: String, val refreshToken: String)
    data class RefreshResponse(val accessToken: String)

    private fun registerAndGetTokens(email: String, password: String): TokenPairResponse {
        val result = mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest(email, password))
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.accessToken") { exists() }
            jsonPath("$.refreshToken") { exists() }
        }.andReturn()

        return objectMapper.readValue(result.response.contentAsString, TokenPairResponse::class.java)
    }

    private fun selectUserProfileByEmail(email: String): Pair<String?, String?> {
        val row = jdbcTemplate.queryForMap(
            """
            SELECT username, profile_image_url
            FROM users
            WHERE email = ?
            """.trimIndent(),
            email
        )
        val username = row["username"] as String?
        val profileImageUrl = row["profile_image_url"] as String?
        return username to profileImageUrl
    }

    @Test
    fun `register login refresh flow works`() {
        val email = "test_${UUID.randomUUID()}@example.com"
        val password = "Abcd1234!" // 정책: 8자+, 영문/숫자/특수문자 포함, 공백 없음

        // 1) 회원가입
        val registerResult = mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest(email, password))
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.accessToken") { exists() }
            jsonPath("$.refreshToken") { exists() }
        }.andReturn()

        val registerTokens = objectMapper.readValue(registerResult.response.contentAsString, TokenPairResponse::class.java)

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

        val loginTokens = objectMapper.readValue(loginResult.response.contentAsString, TokenPairResponse::class.java)
        assertNotEquals(registerTokens.accessToken, loginTokens.accessToken)

        // 3) refresh
        val refreshResult = mockMvc.post("/api/v1/auth/refresh") {
            header("Authorization", "Bearer ${loginTokens.refreshToken}")
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.accessToken") { exists() }
        }.andReturn()

        val refreshed = objectMapper.readValue(refreshResult.response.contentAsString, RefreshResponse::class.java)
        assertNotEquals(loginTokens.accessToken, refreshed.accessToken)
    }

    @Test
    fun `login fails with wrong password`() {
        val email = "test_${UUID.randomUUID()}@example.com"
        val password = "Abcd1234!"
        val wrongPassword = "Abcd1234?wrong"

        mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest(email, password))
        }.andExpect {
            status { isOk() }
        }

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest(email, wrongPassword))
        }.andExpect {
            status { isUnauthorized() }
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

    // ---------------------------
    // 여기부터 "User PATCH" 케이스들
    // ---------------------------

    @Test
    fun `PATCH me updates username only`() {
        val email = "pref_${UUID.randomUUID()}@example.com"
        val password = "Abcd1234!"
        val tokens = registerAndGetTokens(email, password)

        val before = selectUserProfileByEmail(email)
        assertNull(before.first) // username
        assertNull(before.second) // profileImageUrl

        mockMvc.patch("/api/v1/users/me") {
            header("Authorization", "Bearer ${tokens.accessToken}")
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"new_name_${UUID.randomUUID()}"}"""
        }.andExpect {
            status { isNoContent() }
        }

        val after = selectUserProfileByEmail(email)
        assertNotNull(after.first)
        assertNull(after.second)
    }

    @Test
    fun `PATCH me updates profileImageUrl only`() {
        val email = "pref_${UUID.randomUUID()}@example.com"
        val password = "Abcd1234!"
        val tokens = registerAndGetTokens(email, password)

        val url = "https://example.com/${UUID.randomUUID()}.png"

        mockMvc.patch("/api/v1/users/me") {
            header("Authorization", "Bearer ${tokens.accessToken}")
            contentType = MediaType.APPLICATION_JSON
            content = """{"profileImageUrl":"$url"}"""
        }.andExpect {
            status { isNoContent() }
        }

        val after = selectUserProfileByEmail(email)
        assertNull(after.first)
        assertEquals(url, after.second)
    }

    @Test
    fun `PATCH me updates both fields`() {
        val email = "pref_${UUID.randomUUID()}@example.com"
        val password = "Abcd1234!"
        val tokens = registerAndGetTokens(email, password)

        val name = "name_${UUID.randomUUID()}"
        val url = "https://example.com/${UUID.randomUUID()}.jpg"

        mockMvc.patch("/api/v1/users/me") {
            header("Authorization", "Bearer ${tokens.accessToken}")
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"$name","profileImageUrl":"$url"}"""
        }.andExpect {
            status { isNoContent() }
        }

        val after = selectUserProfileByEmail(email)
        assertEquals(name, after.first)
        assertEquals(url, after.second)
    }

    @Test
    fun `PATCH me supports clearing values with null`() {
        val email = "pref_${UUID.randomUUID()}@example.com"
        val password = "Abcd1234!"
        val tokens = registerAndGetTokens(email, password)

        // 먼저 둘 다 채워넣기
        mockMvc.patch("/api/v1/users/me") {
            header("Authorization", "Bearer ${tokens.accessToken}")
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"temp","profileImageUrl":"https://example.com/temp.png"}"""
        }.andExpect {
            status { isNoContent() }
        }

        // null로 비우기
        mockMvc.patch("/api/v1/users/me") {
            header("Authorization", "Bearer ${tokens.accessToken}")
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":null,"profileImageUrl":null}"""
        }.andExpect {
            status { isNoContent() }
        }

        val after = selectUserProfileByEmail(email)
        assertNull(after.first)
        assertNull(after.second)
    }

    @Test
    fun `PATCH me fails when both fields are missing`() {
        val email = "pref_${UUID.randomUUID()}@example.com"
        val password = "Abcd1234!"
        val tokens = registerAndGetTokens(email, password)

        mockMvc.patch("/api/v1/users/me") {
            header("Authorization", "Bearer ${tokens.accessToken}")
            contentType = MediaType.APPLICATION_JSON
            content = """{}"""
        }.andExpect {
            status { isBadRequest() } // ErrorCode.INVALID_REQUEST (400) 기대
        }
    }

    @Test
    fun `PATCH me fails when only unknown fields provided`() {
        val email = "pref_${UUID.randomUUID()}@example.com"
        val password = "Abcd1234!"
        val tokens = registerAndGetTokens(email, password)

        mockMvc.patch("/api/v1/users/me") {
            header("Authorization", "Bearer ${tokens.accessToken}")
            contentType = MediaType.APPLICATION_JSON
            content = """{"foo":"bar"}"""
        }.andExpect {
            status { isBadRequest() } // username/profileImageUrl 둘 다 없으니 INVALID_REQUEST 기대
        }
    }

    @Test
    fun `PATCH me fails without authorization`() {
        mockMvc.patch("/api/v1/users/me") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"no_auth"}"""
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}