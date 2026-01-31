package com.team1.hangsha

import com.team1.hangsha.helper.IntegrationTestBase
import com.team1.hangsha.common.enums.Semester
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class TimetableIntegrationTest : IntegrationTestBase() {

    @Test
    fun `GET timetables - 인증 없으면 401`() {
        mockMvc.perform(get("/api/v1/timetables"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST timetables - 생성 201`() {
        val (_, token) = dataGenerator.generateUserWithAccessToken()

        val req = mapOf(
            "name" to "내 시간표",
            "year" to 2025,
            "semester" to "FALL",
        )

        mockMvc.perform(
            post("/api/v1/timetables")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(req))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.name").value("내 시간표"))
            .andExpect(jsonPath("$.year").value(2025))
            .andExpect(jsonPath("$.semester").value("FALL"))
    }

    @Test
    fun `GET timetables - 필터(year, semester) 적용`() {
        val (user, token) = dataGenerator.generateUserWithAccessToken()

        // 2025: FALL 1개, 2025: SPRING 1개, 2024: FALL 1개
        dataGenerator.generateTimetable(user = user, year = 2025, semester = Semester.FALL, name = "A")
        dataGenerator.generateTimetable(user = user, year = 2025, semester = Semester.SPRING, name = "B")
        dataGenerator.generateTimetable(user = user, year = 2024, semester = Semester.FALL, name = "C")

        mockMvc.perform(
            get("/api/v1/timetables")
                .header("Authorization", bearer(token))
                .param("year", "2025")
                .param("semester", "FALL")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isArray)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].year").value(2025))
            .andExpect(jsonPath("$.items[0].semester").value("FALL"))
            .andExpect(jsonPath("$.items[0].name").value("A"))
    }

    @Test
    fun `PATCH timetables - 이름 수정 200`() {
        val (user, token) = dataGenerator.generateUserWithAccessToken()
        val tt = dataGenerator.generateTimetable(user = user, name = "OLD", year = 2025, semester = Semester.FALL)

        val req = mapOf("name" to "NEW")

        mockMvc.perform(
            patch("/api/v1/timetables/${tt.id}")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(req))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(tt.id!!))
            .andExpect(jsonPath("$.name").value("NEW"))
    }

    @Test
    fun `DELETE timetables - 삭제 204`() {
        val (user, token) = dataGenerator.generateUserWithAccessToken()
        val tt = dataGenerator.generateTimetable(user = user)

        mockMvc.perform(
            delete("/api/v1/timetables/${tt.id}")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `PATCH timetables - 남의 시간표는 404`() {
        val (owner, _) = dataGenerator.generateUserWithAccessToken()
        val (_, otherToken) = dataGenerator.generateUserWithAccessToken()
        val tt = dataGenerator.generateTimetable(user = owner)

        val req = mapOf("name" to "HACK")

        mockMvc.perform(
            patch("/api/v1/timetables/${tt.id}")
                .header("Authorization", bearer(otherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(req))
        )
            // 서비스 정책: 소유자 아니면 NOT_FOUND로 숨김
            .andExpect(status().isNotFound)
    }
}