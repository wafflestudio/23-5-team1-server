package com.team1.hangsha.helper

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = [
        "jwt.secret=v3rys3cr3tk3y_must_be_l0ng_enough_to_be_secure_minimum_256_bits__test",
        "jwt.access-expiration-ms=3600000",
        "jwt.refresh-expiration-ms=1209600000",
    ],
)
abstract class IntegrationTestBase {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var dataGenerator: DataGenerator

    protected fun bearer(token: String) = "Bearer $token"

    protected fun toJson(body: Any): String = objectMapper.writeValueAsString(body)

    @AfterEach
    fun tearDown() {
        // 테스트 격리 전략: 현재 repo들 전부 deleteAll 지원하니까 가장 단순하게 cleanup
        dataGenerator.cleanupAll()
    }
}