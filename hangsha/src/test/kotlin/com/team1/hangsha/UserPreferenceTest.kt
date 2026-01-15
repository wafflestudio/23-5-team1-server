package com.team1.hangsha

import com.fasterxml.jackson.databind.ObjectMapper
import com.team1.hangsha.category.model.Category
import com.team1.hangsha.category.model.CategoryGroup
import com.team1.hangsha.category.repository.CategoryGroupRepository
import com.team1.hangsha.category.repository.CategoryRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.delete
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
class UserPreferenceIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    // 카테고리 시드용 (너희 프로젝트에 실제 레포 이름 맞춰)
    @Autowired lateinit var categoryRepository: CategoryRepository
    @Autowired lateinit var categoryGroupRepository: CategoryGroupRepository

    data class RegisterRequest(val email: String, val password: String)
    data class TokenPairResponse(val accessToken: String, val refreshToken: String)

    data class ReplaceAllInterestCategoriesRequest(val items: List<Item>) {
        data class Item(val categoryId: Long, val priority: Int)
    }

    data class ListInterestCategoryResponse(val items: List<Item>) {
        data class Item(val category: CategoryDto, val priority: Int)
        data class CategoryDto(val id: Long, val groupId: Long, val name: String, val sortOrder: Int)
    }

    private lateinit var accessToken: String
    private lateinit var seededCategoryIds: List<Long>

    @BeforeEach
    fun setUp() {
        // 1) 회원 + 토큰
        val email = "pref_${UUID.randomUUID()}@example.com"
        val password = "Abcd1234!"

        val registerResult = mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest(email, password))
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val tokens = objectMapper.readValue(registerResult.response.contentAsString, TokenPairResponse::class.java)
        accessToken = tokens.accessToken

        // 2) 카테고리 시드 (PUT 검증 통과용)
        // group 1개 + categories 4개 정도 만들어두기
        val group = categoryGroupRepository.save(
            CategoryGroup(
                name = "Group_${UUID.randomUUID()}",
                sortOrder = 1
            )
        )

        val cats = categoryRepository.saveAll(
            listOf(
                Category(groupId = group.id!!, name = "CatA_${UUID.randomUUID()}", sortOrder = 1),
                Category(groupId = group.id!!, name = "CatB_${UUID.randomUUID()}", sortOrder = 2),
                Category(groupId = group.id!!, name = "CatC_${UUID.randomUUID()}", sortOrder = 3),
                Category(groupId = group.id!!, name = "CatD_${UUID.randomUUID()}", sortOrder = 4),
            )
        ).toList()

        seededCategoryIds = cats.map { it.id!! }
    }

    @Test
    fun `put replaces all and get returns ordered list`() {
        val (c1, c2, c3, c4) = seededCategoryIds

        // PUT: 전체 교체 (1..N 연속 priority)
        mockMvc.put("/api/v1/users/me/interest-categories") {
            header("Authorization", "Bearer $accessToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ReplaceAllInterestCategoriesRequest(
                    items = listOf(
                        ReplaceAllInterestCategoriesRequest.Item(categoryId = c3, priority = 1),
                        ReplaceAllInterestCategoriesRequest.Item(categoryId = c1, priority = 2),
                        ReplaceAllInterestCategoriesRequest.Item(categoryId = c4, priority = 3),
                        ReplaceAllInterestCategoriesRequest.Item(categoryId = c2, priority = 4),
                    )
                )
            )
        }.andExpect {
            status { isNoContent() }
        }

        // GET: 목록 + 우선순위 정렬 확인
        val getResult = mockMvc.get("/api/v1/users/me/interest-categories") {
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.items") { isArray() }
            jsonPath("$.items.length()") { value(4) }
            jsonPath("$.items[0].priority") { value(1) }
            jsonPath("$.items[1].priority") { value(2) }
        }.andReturn()

        val body = getResult.response.contentAsString
        val res = objectMapper.readValue(body, ListInterestCategoryResponse::class.java)

        // priority 오름차순으로 정렬되어 오는지
        assertEquals(listOf(1, 2, 3, 4), res.items.map { it.priority })

        // category dto 포함 확인
        res.items.forEach { item ->
            // id/groupId/name/sortOrder가 채워져 있어야 함
            assert(item.category.id > 0)
            assert(item.category.groupId > 0)
            assert(item.category.name.isNotBlank())
        }

        // 우리가 넣은 순서대로 맞는지(1순위는 c3)
        assertEquals(c3, res.items[0].category.id)
    }

    @Test
    fun `delete removes one category and get reflects it`() {
        val (c1, c2, _, _) = seededCategoryIds

        // 먼저 2개만 저장
        mockMvc.put("/api/v1/users/me/interest-categories") {
            header("Authorization", "Bearer $accessToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ReplaceAllInterestCategoriesRequest(
                    items = listOf(
                        ReplaceAllInterestCategoriesRequest.Item(categoryId = c1, priority = 1),
                        ReplaceAllInterestCategoriesRequest.Item(categoryId = c2, priority = 2),
                    )
                )
            )
        }.andExpect { status { isNoContent() } }

        // c1 삭제
        mockMvc.delete("/api/v1/users/me/interest-categories/$c1") {
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isNoContent() }
        }

        // GET 결과는 1개만 남아야 함
        val getResult = mockMvc.get("/api/v1/users/me/interest-categories") {
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(1) }
            jsonPath("$.items[0].category.id") { value(c2) }
        }.andReturn()

        val res = objectMapper.readValue(getResult.response.contentAsString, ListInterestCategoryResponse::class.java)
        assertEquals(listOf(c2), res.items.map { it.category.id })
    }

    @Test
    fun `put rejects duplicate categoryId`() {
        val (c1, c2, _, _) = seededCategoryIds

        mockMvc.put("/api/v1/users/me/interest-categories") {
            header("Authorization", "Bearer $accessToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ReplaceAllInterestCategoriesRequest(
                    items = listOf(
                        ReplaceAllInterestCategoriesRequest.Item(categoryId = c1, priority = 1),
                        ReplaceAllInterestCategoriesRequest.Item(categoryId = c1, priority = 2), // duplicate
                        ReplaceAllInterestCategoriesRequest.Item(categoryId = c2, priority = 3),
                    )
                )
            )
        }.andExpect {
            // DomainException(ErrorCode.INVALID_REQUEST) 매핑이 400이라면
            status { isBadRequest() }
        }
    }

    @Test
    fun `put rejects non-continuous priority`() {
        val (c1, c2, _, _) = seededCategoryIds

        mockMvc.put("/api/v1/users/me/interest-categories") {
            header("Authorization", "Bearer $accessToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ReplaceAllInterestCategoriesRequest(
                    items = listOf(
                        ReplaceAllInterestCategoriesRequest.Item(categoryId = c1, priority = 1),
                        ReplaceAllInterestCategoriesRequest.Item(categoryId = c2, priority = 3), // gap (should be 2)
                    )
                )
            )
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `put rejects non-existing categoryId`() {
        val nonExistingCategoryId = 9_999_999_999L

        mockMvc.put("/api/v1/users/me/interest-categories") {
            header("Authorization", "Bearer $accessToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ReplaceAllInterestCategoriesRequest(
                    items = listOf(
                        ReplaceAllInterestCategoriesRequest.Item(categoryId = nonExistingCategoryId, priority = 1),
                    )
                )
            )
        }.andExpect {
            status { isBadRequest() } // 존재 검증 실패 -> INVALID_REQUEST
        }
    }

    @Test
    fun `unauthorized when no token`() {
        mockMvc.get("/api/v1/users/me/interest-categories")
            .andExpect {
                status { isUnauthorized() }
            }

        mockMvc.put("/api/v1/users/me/interest-categories") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ReplaceAllInterestCategoriesRequest(items = emptyList())
            )
        }.andExpect {
            status { isUnauthorized() }
        }

        mockMvc.delete("/api/v1/users/me/interest-categories/1")
            .andExpect {
                status { isUnauthorized() }
            }
    }
}