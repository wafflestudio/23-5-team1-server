package com.team1.hangsha

import com.team1.hangsha.helper.IntegrationTestBase
import com.team1.hangsha.user.repository.UserExcludedKeywordRepository
import com.team1.hangsha.user.repository.UserInterestCategoryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.beans.factory.annotation.Autowired

class UserPreferenceIntegrationTest : IntegrationTestBase() {

    @Autowired lateinit var userInterestCategoryRepository: UserInterestCategoryRepository
    @Autowired lateinit var userExcludedKeywordRepository: UserExcludedKeywordRepository

    private fun replaceAllBody(vararg pairs: Pair<Long, Int>): String {
        // { "items": [ { "categoryId": 1, "priority": 1 }, ... ] }
        val body = mapOf(
            "items" to pairs.map { (categoryId, priority) ->
                mapOf("categoryId" to categoryId, "priority" to priority)
            }
        )
        return toJson(body)
    }

    @Test
    fun `PUT replaces all and GET returns ordered list`() {
        val (_, token) = dataGenerator.generateUserWithAccessToken()

        val group = dataGenerator.generateCategoryGroup(name = "group", sortOrder = 1)
        val c1 = dataGenerator.generateCategory(group = group, name = "c1", sortOrder = 1)
        val c2 = dataGenerator.generateCategory(group = group, name = "c2", sortOrder = 2)
        val c3 = dataGenerator.generateCategory(group = group, name = "c3", sortOrder = 3)
        val c4 = dataGenerator.generateCategory(group = group, name = "c4", sortOrder = 4)

        val id1 = c1.id!!
        val id2 = c2.id!!
        val id3 = c3.id!!
        val id4 = c4.id!!

        // PUT: 전체 교체 (priority 1..N 연속)
        mockMvc.perform(
            put("/api/v1/users/me/interest-categories")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    replaceAllBody(
                        id3 to 1,
                        id1 to 2,
                        id4 to 3,
                        id2 to 4,
                    )
                )
        )
            .andExpect(status().isNoContent)

        // GET: 우선순위 정렬/DTO 포함 확인
        val getResult = mockMvc.perform(
            get("/api/v1/users/me/interest-categories")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.items").isArray)
            .andExpect(jsonPath("$.items.length()").value(4))
            .andExpect(jsonPath("$.items[0].priority").value(1))
            .andExpect(jsonPath("$.items[1].priority").value(2))
            .andExpect(jsonPath("$.items[2].priority").value(3))
            .andExpect(jsonPath("$.items[3].priority").value(4))
            // category dto 들어오는지
            .andExpect(jsonPath("$.items[0].category.id").isNumber)
            .andExpect(jsonPath("$.items[0].category.groupId").isNumber)
            .andExpect(jsonPath("$.items[0].category.name").isString)
            .andExpect(jsonPath("$.items[0].category.sortOrder").isNumber)
            .andReturn()

        // 1순위가 id3인지
        val root = objectMapper.readTree(getResult.response.contentAsString)
        val priorities = root["items"].map { it["priority"].asInt() }
        assertEquals(listOf(1, 2, 3, 4), priorities)

        val firstCategoryId = root["items"][0]["category"]["id"].asLong()
        assertEquals(id3, firstCategoryId)
    }

    @Test
    fun `DELETE removes one category and GET reflects it`() {
        val (_, token) = dataGenerator.generateUserWithAccessToken()

        val group = dataGenerator.generateCategoryGroup(name = "group", sortOrder = 1)
        val c1 = dataGenerator.generateCategory(group = group, name = "c1", sortOrder = 1)
        val c2 = dataGenerator.generateCategory(group = group, name = "c2", sortOrder = 2)

        val id1 = c1.id!!
        val id2 = c2.id!!

        // 먼저 2개 저장
        mockMvc.perform(
            put("/api/v1/users/me/interest-categories")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(replaceAllBody(id1 to 1, id2 to 2))
        )
            .andExpect(status().isNoContent)

        // id1 삭제
        mockMvc.perform(
            delete("/api/v1/users/me/interest-categories/$id1")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isNoContent)

        // GET 결과는 id2만 남아야 함
        mockMvc.perform(
            get("/api/v1/users/me/interest-categories")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].category.id").value(id2))
            .andExpect(jsonPath("$.items[0].priority").value(1)) // 삭제 후 재정렬/당김 스펙이면 1이 되겠지? (스펙에 맞게 조정)
    }

    @Test
    fun `PUT rejects duplicate categoryId`() {
        val (_, token) = dataGenerator.generateUserWithAccessToken()

        val group = dataGenerator.generateCategoryGroup(name = "group", sortOrder = 1)
        val c1 = dataGenerator.generateCategory(group = group, name = "c1", sortOrder = 1)
        val c2 = dataGenerator.generateCategory(group = group, name = "c2", sortOrder = 2)

        val id1 = c1.id!!
        val id2 = c2.id!!

        mockMvc.perform(
            put("/api/v1/users/me/interest-categories")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(replaceAllBody(id1 to 1, id1 to 2, id2 to 3))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT rejects non-continuous priority`() {
        val (_, token) = dataGenerator.generateUserWithAccessToken()

        val group = dataGenerator.generateCategoryGroup(name = "group", sortOrder = 1)
        val c1 = dataGenerator.generateCategory(group = group, name = "c1", sortOrder = 1)
        val c2 = dataGenerator.generateCategory(group = group, name = "c2", sortOrder = 2)

        val id1 = c1.id!!
        val id2 = c2.id!!

        mockMvc.perform(
            put("/api/v1/users/me/interest-categories")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(replaceAllBody(id1 to 1, id2 to 3)) // gap: 2가 없음
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT rejects non-existing categoryId`() {
        val (_, token) = dataGenerator.generateUserWithAccessToken()

        val nonExistingCategoryId = 9_999_999_999L

        mockMvc.perform(
            put("/api/v1/users/me/interest-categories")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(replaceAllBody(nonExistingCategoryId to 1))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `unauthorized when no token`() {
        mockMvc.perform(get("/api/v1/users/me/interest-categories"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            put("/api/v1/users/me/interest-categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(mapOf("items" to emptyList<Any>())))
        )
            .andExpect(status().isUnauthorized)

        mockMvc.perform(delete("/api/v1/users/me/interest-categories/1"))
            .andExpect(status().isUnauthorized)
    }

    data class ListExcludedKeywordResponse(val items: List<Item>) {
        data class Item(val id: Long, val keyword: String, val createdAt: String)
    }

    private fun list(token: String): ListExcludedKeywordResponse {
        val res = mockMvc.perform(
            get("/api/v1/users/me/excluded-keywords")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andReturn()

        return objectMapper.readValue(res.response.contentAsString, ListExcludedKeywordResponse::class.java)
    }

    @Test
    fun `GET excluded-keywords - 처음엔 빈 배열`() {
        val (_, token) = dataGenerator.generateUserWithAccessToken()

        mockMvc.perform(
            get("/api/v1/users/me/excluded-keywords")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isArray)
            .andExpect(jsonPath("$.items.length()").value(0))
    }

    @Test
    fun `POST excluded-keywords - 성공하면 201, GET에 trim된 키워드가 보인다`() {
        val (user, token) = dataGenerator.generateUserWithAccessToken()

        mockMvc.perform(
            post("/api/v1/users/me/excluded-keywords")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"keyword":"   apple  "}""")
        )
            .andExpect(status().isCreated)

        // DB에도 trim된 keyword로 저장됐는지 확인
        assertEquals(1, userExcludedKeywordRepository.countByUserIdAndKeyword(requireNotNull(user.id), "apple"))

        val body = list(token)
        assertEquals(1, body.items.size)
        assertEquals("apple", body.items[0].keyword)
        assertTrue(body.items[0].id > 0)
        assertTrue(body.items[0].createdAt.isNotBlank())
    }

    @Test
    fun `POST excluded-keywords - 같은 키워드 중복 추가해도 멱등으로 1개만 유지`() {
        val (user, token) = dataGenerator.generateUserWithAccessToken()

        mockMvc.perform(
            post("/api/v1/users/me/excluded-keywords")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"keyword":"banana"}""")
        )
            .andExpect(status().isCreated)

        // 같은 키워드 재요청 (서비스가 return 처리 → 컨트롤러는 201 유지)
        mockMvc.perform(
            post("/api/v1/users/me/excluded-keywords")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"keyword":"banana"}""")
        )
            .andExpect(status().isCreated)

        // DB에 1개만 있어야 함
        assertEquals(1, userExcludedKeywordRepository.countByUserIdAndKeyword(requireNotNull(user.id), "banana"))

        val body = list(token)
        assertEquals(1, body.items.size)
        assertEquals("banana", body.items[0].keyword)
    }

    @Test
    fun `POST excluded-keywords - blank면 400`() {
        val (_, token) = dataGenerator.generateUserWithAccessToken()

        // @NotBlank + 서비스 trim/blank 체크 둘 다 걸릴 수 있음
        mockMvc.perform(
            post("/api/v1/users/me/excluded-keywords")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"keyword":"   "}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET excluded-keywords - created_at desc, id desc 정렬`() {
        val (_, token) = dataGenerator.generateUserWithAccessToken()

        // 순서대로 넣었을 때, 리스트는 최신이 먼저 와야 함
        mockMvc.perform(
            post("/api/v1/users/me/excluded-keywords")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"keyword":"k1"}""")
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/v1/users/me/excluded-keywords")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"keyword":"k2"}""")
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/v1/users/me/excluded-keywords")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"keyword":"k3"}""")
        ).andExpect(status().isCreated)

        val body = list(token)
        assertEquals(listOf("k3", "k2", "k1"), body.items.map { it.keyword })
    }

    @Test
    fun `DELETE excluded-keywords - 내 키워드 삭제되면 204, GET에서 제거됨`() {
        val (_, token) = dataGenerator.generateUserWithAccessToken()

        // 추가
        mockMvc.perform(
            post("/api/v1/users/me/excluded-keywords")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"keyword":"to-delete"}""")
        )
            .andExpect(status().isCreated)

        val before = list(token)
        assertEquals(1, before.items.size)
        val id = before.items[0].id

        // 삭제
        mockMvc.perform(
            delete("/api/v1/users/me/excluded-keywords/$id")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isNoContent)

        val after = list(token)
        assertEquals(0, after.items.size)
    }

    @Test
    fun `DELETE excluded-keywords - 남의 id로 삭제 시도하면 400`() {
        val (_, tokenA) = dataGenerator.generateUserWithAccessToken()
        val (_, tokenB) = dataGenerator.generateUserWithAccessToken()

        // A가 키워드 추가
        mockMvc.perform(
            post("/api/v1/users/me/excluded-keywords")
                .header("Authorization", bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"keyword":"secret"}""")
        )
            .andExpect(status().isCreated)

        val aList = list(tokenA)
        val aId = aList.items.single().id

        // B가 A의 excludedKeywordId로 삭제 시도
        mockMvc.perform(
            delete("/api/v1/users/me/excluded-keywords/$aId")
                .header("Authorization", bearer(tokenB))
        )
            .andExpect(status().isBadRequest)

        // A의 목록은 그대로 남아야 함
        val aAfter = list(tokenA)
        assertEquals(1, aAfter.items.size)
        assertEquals("secret", aAfter.items[0].keyword)
    }

    @Test
    fun `DELETE excluded-keywords - 존재하지 않는 id 삭제 시 400`() {
        val (_, token) = dataGenerator.generateUserWithAccessToken()

        mockMvc.perform(
            delete("/api/v1/users/me/excluded-keywords/999999999")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `ExcludedKeyword API - 인증 없으면 401`() {
        mockMvc.perform(get("/api/v1/users/me/excluded-keywords"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            post("/api/v1/users/me/excluded-keywords")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"keyword":"x"}""")
        )
            .andExpect(status().isUnauthorized)

        mockMvc.perform(delete("/api/v1/users/me/excluded-keywords/1"))
            .andExpect(status().isUnauthorized)
    }
}