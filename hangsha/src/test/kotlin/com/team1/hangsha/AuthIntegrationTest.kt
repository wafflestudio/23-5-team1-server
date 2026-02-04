package com.team1.hangsha

import com.team1.hangsha.helper.IntegrationTestBase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import jakarta.servlet.http.Cookie
import java.util.UUID

@TestPropertySource(
    properties = [
        // 자동 refresh 흐름 테스트를 위해 access를 짧게 (1초)
        "jwt.access-expiration-ms=1000",
        "jwt.refresh-expiration-ms=1209600000",
    ],
)
class AuthIntegrationTest : IntegrationTestBase() {

    data class RegisterRequest(val email: String, val password: String)
    data class LoginRequest(val email: String, val password: String)

    data class RegisterResponse(val accessToken: String)
    data class LoginResponse(val accessToken: String)
    data class RefreshResponse(val accessToken: String)

    private fun newEmail(prefix: String = "test"): String =
        "${prefix}_${UUID.randomUUID()}@example.com"

    private fun validPassword(): String = "Abcd1234!"

    /**
     * Set-Cookie 헤더들에서 refreshToken=... 를 찾아
     * "refreshToken=...." (세미콜론 전까지) 형태로 반환
     */
    private fun extractRefreshCookie(setCookies: List<String>): String {
        val raw = setCookies.firstOrNull { it.startsWith("refreshToken=") }
            ?: error("Expected Set-Cookie for refreshToken, but got: $setCookies")

        // "refreshToken=xxx; Path=/; HttpOnly; ..." -> "refreshToken=xxx"
        return raw.substringBefore(";")
    }

    /**
     * Cookie 헤더 값으로 넣기 위한 문자열
     * 예: "refreshToken=xxx"
     */
    private fun cookieHeader(refreshCookiePair: String): String = refreshCookiePair

    /**
     * register 호출 → (accessToken, refreshCookiePair)
     * refreshCookiePair는 "refreshToken=..." 형태
     */
    private fun register(email: String, password: String): Pair<String, String> {
        val res = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(RegisterRequest(email, password)))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").exists())
            .andReturn()

        val access = objectMapper.readValue(res.response.contentAsString, RegisterResponse::class.java).accessToken
        val refreshValue = refreshTokenValueFrom(res)

        return access to refreshValue
    }

    private fun login(email: String, password: String): Pair<String, String> {
        val res = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(LoginRequest(email, password)))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").exists())
            .andReturn()

        val access = objectMapper.readValue(res.response.contentAsString, LoginResponse::class.java).accessToken
        val refreshValue = refreshTokenValueFrom(res)

        return access to refreshValue
    }

    private fun refresh(refreshTokenValue: String): Pair<String, String> {
        val res = mockMvc.perform(
            post("/api/v1/auth/refresh")
                .cookie(Cookie("refreshToken", refreshTokenValue))   // ✅ 핵심
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").exists())
            .andReturn()

        val access = objectMapper.readValue(res.response.contentAsString, RefreshResponse::class.java).accessToken
        val newRefreshValue = refreshTokenValueFrom(res)

        return access to newRefreshValue
    }

    private fun countRefreshTokenRowsByEmail(email: String): Int {
        return jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM refresh_tokens rt
            JOIN users u ON u.id = rt.user_id
            WHERE u.email = ?
            """.trimIndent(),
            Int::class.java,
            email
        ) ?: 0
    }

    private fun countActiveRefreshTokenRowsByEmail(email: String): Int {
        return jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM refresh_tokens rt
            JOIN users u ON u.id = rt.user_id
            WHERE u.email = ?
              AND rt.revoked_at IS NULL
              AND rt.expires_at > CURRENT_TIMESTAMP(6)
            """.trimIndent(),
            Int::class.java,
            email
        ) ?: 0
    }

    private fun refreshTokenValueFrom(res: org.springframework.test.web.servlet.MvcResult): String {
        val c = res.response.getCookie("refreshToken")
            ?: error("refreshToken cookie not found in response")
        return c.value
    }

    // ---------------------------------------------------------
    // Auth core flows
    // ---------------------------------------------------------

    @Test
    fun `POST register issues access token in body and refresh token in cookie and stores refresh row`() {
        val email = newEmail("reg")
        val password = validPassword()

        val (access, refreshCookie) = register(email, password)

        assertTrue(access.isNotBlank())
        assertTrue(refreshCookie.isNotBlank())

        assertTrue(countRefreshTokenRowsByEmail(email) >= 1)
        assertTrue(countActiveRefreshTokenRowsByEmail(email) >= 1)
    }

    @Test
    fun `POST login issues access token in body and refresh token in cookie`() {
        val email = newEmail("login")
        val password = validPassword()

        register(email, password)
        val (access, refreshCookie) = login(email, password)

        assertTrue(access.isNotBlank())
        assertTrue(refreshCookie.isNotBlank())

        // register 1개 + login 1개 이상
        assertTrue(countRefreshTokenRowsByEmail(email) >= 2)
        assertTrue(countActiveRefreshTokenRowsByEmail(email) >= 1)
    }

    @Test
    fun `POST refresh rotates refresh token - old refresh becomes revoked and new cookie is issued`() {
        val email = newEmail("rot")
        val password = validPassword()

        val (_, refreshCookie1) = register(email, password)

        val (newAccess, refreshCookie2) = refresh(refreshCookie1)

        assertTrue(newAccess.isNotBlank())
        assertNotEquals(refreshCookie1, refreshCookie2)

        // 회전 후에도 새 토큰이 active로 존재해야 함
        assertTrue(countActiveRefreshTokenRowsByEmail(email) >= 1)
    }

    @Test
    fun `POST refresh with already-rotated old refresh token should fail`() {
        val email = newEmail("rotfail")
        val password = validPassword()

        val (_, refreshCookie1) = register(email, password)
        val (_, refreshCookie2) = refresh(refreshCookie1)

        // old refresh로 또 refresh 시도 => 401
        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .cookie(Cookie("refreshToken", refreshCookie1))
        )
            .andExpect(status().isUnauthorized)

        // 최신 refresh로는 성공해야 함
        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .cookie(Cookie("refreshToken", refreshCookie2))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `POST logout revokes refresh token and clears cookie`() {
        val email = newEmail("logout")
        val password = validPassword()

        val (_, refreshCookie) = register(email, password)
        val activeBefore = countActiveRefreshTokenRowsByEmail(email)
        assertTrue(activeBefore >= 1)

        val res = mockMvc.perform(
            post("/api/v1/auth/logout")
                .cookie(Cookie("refreshToken", refreshCookie))
        )
            .andExpect(status().isNoContent)
            .andReturn()

        val setCookies = res.response.getHeaders(HttpHeaders.SET_COOKIE)
        val cleared = setCookies.firstOrNull { it.startsWith("refreshToken=") }
            ?: error("logout should set refreshToken clear cookie")

        // clearRefreshCookie()가 Max-Age=0 또는 Expires=... 과 같이 내려야 정상
        assertTrue(
            cleared.contains("Max-Age=0") || cleared.contains("Expires="),
            "Expected clear cookie (Max-Age=0 or Expires=...), got: $cleared"
        )

        // DB에서 active refresh가 줄어든다(보통 0 됨)
        val activeAfter = countActiveRefreshTokenRowsByEmail(email)
        assertTrue(activeAfter < activeBefore, "Expected active refresh tokens to decrease after logout")
    }

    // ---------------------------------------------------------
    // access 만료 -> refresh -> 재시도
    // ---------------------------------------------------------

    @Test
    fun `expired access token can be recovered by refresh then retry protected api`() {
        val email = newEmail("auto")
        val password = validPassword()

        val (access1, refresh1) = register(email, password)

        Thread.sleep(1200)

        mockMvc.perform(
            get("/api/v1/memos")
                .header(HttpHeaders.AUTHORIZATION, bearer(access1))
        ).andExpect(status().isUnauthorized)

        val (access2, refresh2) = refresh(refresh1)
        assertNotEquals(access1, access2)
        assertNotEquals(refresh1, refresh2)

        mockMvc.perform(
            get("/api/v1/memos")
                .header(HttpHeaders.AUTHORIZATION, bearer(access2))
        ).andExpect(status().isOk)
    }

    // ---------------------------------------------------------
    // User PATCH (기존 파일 내용 포함)
    // ---------------------------------------------------------

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
    fun `PATCH me updates username only`() {
        val email = newEmail("pref")
        val password = validPassword()
        val (access, _) = register(email, password)

        val before = selectUserProfileByEmail(email)
        assertNull(before.first)
        assertNull(before.second)

        mockMvc.perform(
            patch("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(access))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"new_name_${UUID.randomUUID()}"}""")
        )
            .andExpect(status().isNoContent)

        val after = selectUserProfileByEmail(email)
        assertNotNull(after.first)
        assertNull(after.second)
    }

    @Test
    fun `PATCH me updates profileImageUrl only`() {
        val email = newEmail("pref")
        val password = validPassword()
        val (access, _) = register(email, password)

        val url = "https://example.com/${UUID.randomUUID()}.png"

        mockMvc.perform(
            patch("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(access))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"profileImageUrl":"$url"}""")
        )
            .andExpect(status().isNoContent)

        val after = selectUserProfileByEmail(email)
        assertNull(after.first)
        assertEquals(url, after.second)
    }

    @Test
    fun `PATCH me updates both fields`() {
        val email = newEmail("pref")
        val password = validPassword()
        val (access, _) = register(email, password)

        val name = "name_${UUID.randomUUID()}"
        val url = "https://example.com/${UUID.randomUUID()}.jpg"

        mockMvc.perform(
            patch("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(access))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"$name","profileImageUrl":"$url"}""")
        )
            .andExpect(status().isNoContent)

        val after = selectUserProfileByEmail(email)
        assertEquals(name, after.first)
        assertEquals(url, after.second)
    }

    @Test
    fun `PATCH me supports clearing values with null`() {
        val email = newEmail("pref")
        val password = validPassword()
        val (access, _) = register(email, password)

        mockMvc.perform(
            patch("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(access))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"temp","profileImageUrl":"https://example.com/temp.png"}""")
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            patch("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(access))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":null,"profileImageUrl":null}""")
        )
            .andExpect(status().isNoContent)

        val after = selectUserProfileByEmail(email)
        assertNull(after.first)
        assertNull(after.second)
    }

    @Test
    fun `PATCH me fails when both fields are missing`() {
        val email = newEmail("pref")
        val password = validPassword()
        val (access, _) = register(email, password)

        mockMvc.perform(
            patch("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(access))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PATCH me fails when only unknown fields provided`() {
        val email = newEmail("pref")
        val password = validPassword()
        val (access, _) = register(email, password)

        mockMvc.perform(
            patch("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(access))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"foo":"bar"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PATCH me fails without authorization`() {
        mockMvc.perform(
            patch("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"no_auth"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `login fails with wrong password`() {
        val email = newEmail("wrongpw")
        val password = validPassword()
        val wrongPassword = "Abcd1234?wrong"

        register(email, password)

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(LoginRequest(email, wrongPassword)))
        )
            .andExpect(status().isUnauthorized)
    }
}