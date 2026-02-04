package com.team1.hangsha.helper

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = [
        "jwt.secret=v3rys3cr3tk3y_must_be_l0ng_enough_to_be_secure_minimum_256_bits__test",
        "jwt.access-expiration-ms=3600000",
        "jwt.refresh-expiration-ms=1209600000",

        "GOOGLE_CLIENT_ID=test-google-client-id",
        "GOOGLE_CLIENT_SECRET=test-google-client-secret",
        "NAVER_CLIENT_ID=test-naver-client-id",
        "NAVER_CLIENT_SECRET=test-naver-client-secret",
        "KAKAO_CLIENT_ID=test-kakao-client-id",
        "KAKAO_CLIENT_SECRET=test-kakao-client-secret",
    ],
)
abstract class IntegrationTestBase {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var dataGenerator: DataGenerator
    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    protected fun bearer(token: String) = "Bearer $token"
    protected fun toJson(body: Any): String = objectMapper.writeValueAsString(body)

    /**
     * Spring MockMvc response 에서 Set-Cookie 헤더 중 refreshToken 쿠키를 찾아 "refreshToken=..." 형태로 반환.
     * (Path/HttpOnly/SameSite 등 속성은 제외하고, 요청용 Cookie 헤더에 그대로 쓰기 좋게)
     */
    protected fun extractRefreshCookie(setCookieHeaders: Collection<String>): String {
        val raw = setCookieHeaders.firstOrNull { it.startsWith("refreshToken=") }
            ?: throw IllegalStateException("No refreshToken Set-Cookie header found: $setCookieHeaders")

        // "refreshToken=xxx; Path=/...; HttpOnly; Secure; SameSite=Lax"
        val pair = raw.substringBefore(";").trim()
        if (!pair.startsWith("refreshToken=")) {
            throw IllegalStateException("Invalid refreshToken cookie header: $raw")
        }
        return pair
    }

    /** MockMvc에 넣을 Cookie 헤더 값 (예: "refreshToken=xxx") */
    protected fun cookieHeader(vararg cookies: String): String =
        cookies.joinToString("; ")

    @AfterEach
    fun tearDown() {
        dataGenerator.cleanupAll()
    }
}