package com.team1.hangsha

import com.team1.hangsha.event.repository.EventRepository
import com.team1.hangsha.helper.IntegrationTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class BookmarkIntegrationTest : IntegrationTestBase() {

    @Autowired lateinit var eventRepository: EventRepository

    @Test
    fun `POST bookmark - 성공하면 204`() {
        val (_, token) = dataGenerator.generateUserWithAccessToken()
        val event = dataGenerator.generateEvent()

        mockMvc.perform(
            post("/api/v1/events/${event.id}/bookmark")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `POST bookmark - 이미 북마크여도 204`() {
        val (_, token) = dataGenerator.generateUserWithAccessToken()
        val event = dataGenerator.generateEvent()

        mockMvc.perform(
            post("/api/v1/events/${event.id}/bookmark")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isNoContent)

        // 같은 요청 한 번 더
        mockMvc.perform(
            post("/api/v1/events/${event.id}/bookmark")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `POST bookmark - 없는 이벤트면 404`() {
        val (_, token) = dataGenerator.generateUserWithAccessToken()

        mockMvc.perform(
            post("/api/v1/events/999999/bookmark")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET my bookmarks - 목록 조회`() {
        val (_, token) = dataGenerator.generateUserWithAccessToken()
        val e1 = dataGenerator.generateEvent(title = "event-1")
        val e2 = dataGenerator.generateEvent(title = "event-2")

        mockMvc.perform(
            post("/api/v1/events/${e1.id}/bookmark")
                .header("Authorization", bearer(token))
        )
        mockMvc.perform(
            post("/api/v1/events/${e2.id}/bookmark")
                .header("Authorization", bearer(token))
        )

        mockMvc.perform(
            get("/api/v1/users/me/bookmarks")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.items").isArray)
            .andExpect(jsonPath("$.items[0].isBookmarked").value(true))
    }

    @Test
    fun `DELETE bookmark - 삭제 후 목록에서 제거됨`() {
        val (_, token) = dataGenerator.generateUserWithAccessToken()
        val event = dataGenerator.generateEvent()

        mockMvc.perform(
            post("/api/v1/events/${event.id}/bookmark")
                .header("Authorization", bearer(token))
        )

        mockMvc.perform(
            delete("/api/v1/events/${event.id}/bookmark")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            get("/api/v1/users/me/bookmarks")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(0))
    }

    @Test
    fun `Bookmark API - 인증 없으면 401`() {
        val event = dataGenerator.generateEvent()

        mockMvc.perform(
            post("/api/v1/events/${event.id}/bookmark")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `apply_count - 북마크 추가-중복추가-삭제-중복삭제 시 apply_count가 0-1-1-0-0 으로 유지`() {
        val (_, token) = dataGenerator.generateUserWithAccessToken()

        // 시작 applyCount를 0으로 강제
        val event = dataGenerator.generateEvent(applyCount = 0)
        val eventId = requireNotNull(event.id)

        fun currentApplyCount(): Int {
            val refreshed = eventRepository.findById(eventId)
                .orElseThrow { IllegalStateException("event not found: $eventId") }
            return refreshed.applyCount
        }

        // 초기값 확인
        assertEquals(0, currentApplyCount())

        // 1) 북마크 추가 -> +1
        mockMvc.perform(
            post("/api/v1/events/$eventId/bookmark")
                .header("Authorization", bearer(token))
        ).andExpect(status().isNoContent)

        assertEquals(1, currentApplyCount())

        // 2) 중복 추가 -> 그대로 1 (insertIgnore이므로 증가하면 안 됨)
        mockMvc.perform(
            post("/api/v1/events/$eventId/bookmark")
                .header("Authorization", bearer(token))
        ).andExpect(status().isNoContent)

        assertEquals(1, currentApplyCount())

        // 3) 삭제 -> -1
        mockMvc.perform(
            delete("/api/v1/events/$eventId/bookmark")
                .header("Authorization", bearer(token))
        ).andExpect(status().isNoContent)

        assertEquals(0, currentApplyCount())

        // 4) 중복 삭제 -> 그대로 0 (GREATEST 적용 및 deleted==0이면 감소 안 되어야 함)
        mockMvc.perform(
            delete("/api/v1/events/$eventId/bookmark")
                .header("Authorization", bearer(token))
        ).andExpect(status().isNoContent)

        assertEquals(0, currentApplyCount())
    }

    @Test
    fun `다른 유저 격리 - userA 북마크가 userB 목록에 섞이지 않는다`() {
        val (_, tokenA) = dataGenerator.generateUserWithAccessToken()
        val (_, tokenB) = dataGenerator.generateUserWithAccessToken()
        val event = dataGenerator.generateEvent()

        // userA가 북마크
        mockMvc.perform(
            post("/api/v1/events/${event.id}/bookmark")
                .header("Authorization", bearer(tokenA))
        ).andExpect(status().isNoContent)

        // userB로 조회하면 0이어야 함
        mockMvc.perform(
            get("/api/v1/users/me/bookmarks")
                .header("Authorization", bearer(tokenB))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(0))
            .andExpect(jsonPath("$.items").isArray)
            .andExpect(jsonPath("$.items.length()").value(0))
    }

    @Test
    fun `DELETE bookmark - 북마크 안 한 상태에서 삭제해도 204 (멱등성)`() {
        val (_, token) = dataGenerator.generateUserWithAccessToken()
        val event = dataGenerator.generateEvent()

        // 아직 북마크 안했는데 delete
        mockMvc.perform(
            delete("/api/v1/events/${event.id}/bookmark")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isNoContent)

        // 목록도 비어있음
        mockMvc.perform(
            get("/api/v1/users/me/bookmarks")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(0))
    }

    @Test
    fun `DELETE bookmark - userB가 DELETE해도 userA의 북마크는 유지되고 apply_count도 변하지 않는다`() {
        val (_, tokenA) = dataGenerator.generateUserWithAccessToken()
        val (_, tokenB) = dataGenerator.generateUserWithAccessToken()

        // applyCount 0부터 시작하면 검증이 더 명확
        val event = dataGenerator.generateEvent(applyCount = 0)
        val eventId = requireNotNull(event.id)

        fun currentApplyCount(): Int {
            val refreshed = eventRepository.findById(eventId)
                .orElseThrow { IllegalStateException("event not found: $eventId") }
            return refreshed.applyCount
        }

        // 1) userA가 북마크 -> apply_count = 1
        mockMvc.perform(
            post("/api/v1/events/$eventId/bookmark")
                .header("Authorization", bearer(tokenA))
        ).andExpect(status().isNoContent)

        assertEquals(1, currentApplyCount())

        // 2) userB가 삭제 시도 -> (대개 204 나올 것)
        mockMvc.perform(
            delete("/api/v1/events/$eventId/bookmark")
                .header("Authorization", bearer(tokenB))
        ).andExpect(status().isNoContent)

        // 3) userA 목록에는 그대로 있어야 함
        mockMvc.perform(
            get("/api/v1/users/me/bookmarks")
                .header("Authorization", bearer(tokenA))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items").isArray)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(eventId))
            .andExpect(jsonPath("$.items[0].isBookmarked").value(true))

        // 4) userB 목록은 비어있어야 함
        mockMvc.perform(
            get("/api/v1/users/me/bookmarks")
                .header("Authorization", bearer(tokenB))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(0))
            .andExpect(jsonPath("$.items.length()").value(0))

        // 5) apply_count도 1 유지(남이 지운다고 줄면 안 됨)
        assertEquals(1, currentApplyCount())
    }
}