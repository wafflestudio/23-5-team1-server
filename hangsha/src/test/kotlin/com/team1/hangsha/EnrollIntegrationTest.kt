package com.team1.hangsha

import com.team1.hangsha.helper.IntegrationTestBase
import com.team1.hangsha.common.enums.Semester
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class EnrollIntegrationTest : IntegrationTestBase() {

    @Test
    fun `GET enrolls - 인증 없으면 401`() {
        mockMvc.perform(get("/api/v1/timetables/1/enrolls"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET enrolls - 자기 timetableId면 200`() {
        val (owner, _) = dataGenerator.generateUserWithAccessToken()
        val (otherUser, otherToken) = dataGenerator.generateUserWithAccessToken()

        val otherTt = dataGenerator.generateTimetable(user = otherUser, year = 2025, semester = Semester.FALL)

        mockMvc.perform(
            get("/api/v1/timetables/${otherTt.id}/enrolls")
                .header("Authorization", bearer(otherToken)) // otherToken이 아닌 ownerToken으로 접근해야 "남의 것" 테스트가 됨
        )
            .andExpect(status().isOk) // 이건 본인 접근이라 OK

        val (_, ownerToken) = owner to "" // ownerToken 다시 만들기 어려우니 아래처럼 owner를 새로 발급하는게 안전
    }

    @Test
    fun `GET enrolls - 남의 timetableId면 404 (정확 버전)`() {
        val (owner, ownerToken) = dataGenerator.generateUserWithAccessToken()
        val (otherUser, _) = dataGenerator.generateUserWithAccessToken()

        val otherTt = dataGenerator.generateTimetable(user = otherUser, year = 2025, semester = Semester.FALL)

        mockMvc.perform(
            get("/api/v1/timetables/${otherTt.id}/enrolls")
                .header("Authorization", bearer(ownerToken))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET enroll - timetableId와 enrollId가 불일치하면 404`() {
        val (user, token) = dataGenerator.generateUserWithAccessToken()

        val ttA = dataGenerator.generateTimetable(user = user, year = 2025, semester = Semester.FALL)
        val ttB = dataGenerator.generateTimetable(user = user, year = 2025, semester = Semester.FALL)

        val created = mockMvc.perform(
            post("/api/v1/timetables/${ttA.id}/enrolls/custom")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    toJson(
                        mapOf(
                            "year" to 2025,
                            "semester" to "FALL",
                            "courseTitle" to "A에만있는강의",
                            "timeSlots" to listOf(
                                mapOf("dayOfWeek" to "MON", "startAt" to 540, "endAt" to 615),
                            ),
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andReturn()

        val enrollId = objectMapper.readTree(created.response.contentAsString)["enrollId"].asLong()

        mockMvc.perform(
            get("/api/v1/timetables/${ttB.id}/enrolls/$enrollId")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isNotFound)
    }


    @Test
    fun `POST enrolls custom - 남의 timetableId면 404`() {
        val (owner, ownerToken) = dataGenerator.generateUserWithAccessToken()
        val (otherUser, _) = dataGenerator.generateUserWithAccessToken()

        val otherTt = dataGenerator.generateTimetable(user = otherUser, year = 2025, semester = Semester.FALL)

        val req = mapOf(
            "year" to 2025,
            "semester" to "FALL",
            "courseTitle" to "남의시간표에추가시도",
            "timeSlots" to listOf(
                mapOf("dayOfWeek" to "MON", "startAt" to 540, "endAt" to 615),
            ),
        )

        mockMvc.perform(
            post("/api/v1/timetables/${otherTt.id}/enrolls/custom")
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(req))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST enrolls custom - 시간표 학기년도 불일치면 400`() {
        val (user, token) = dataGenerator.generateUserWithAccessToken()
        val tt = dataGenerator.generateTimetable(user = user, year = 2025, semester = Semester.FALL)

        val req = mapOf(
            "year" to 2024, // mismatch
            "semester" to "FALL",
            "courseTitle" to "커스텀강의",
            "timeSlots" to listOf(
                mapOf("dayOfWeek" to "MON", "startAt" to 540, "endAt" to 615)
            ),
        )

        mockMvc.perform(
            post("/api/v1/timetables/${tt.id}/enrolls/custom")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(req))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST enrolls custom - 생성 201 후 list에서 조회`() {
        val (user, token) = dataGenerator.generateUserWithAccessToken()
        val tt = dataGenerator.generateTimetable(user = user, year = 2025, semester = Semester.FALL)

        val req = mapOf(
            "year" to 2025,
            "semester" to "FALL",
            "courseTitle" to "커스텀강의",
            "timeSlots" to listOf(
                mapOf("dayOfWeek" to "MON", "startAt" to 540, "endAt" to 615),
                mapOf("dayOfWeek" to "WED", "startAt" to 540, "endAt" to 615),
            ),
            "credit" to 3,
            "instructor" to "교수님",
        )

        // 생성
        val createRes = mockMvc.perform(
            post("/api/v1/timetables/${tt.id}/enrolls/custom")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(req))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.enrollId").isNumber)
            .andExpect(jsonPath("$.course.courseTitle").value("커스텀강의"))
            .andReturn()

        // 목록
        mockMvc.perform(
            get("/api/v1/timetables/${tt.id}/enrolls")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isArray)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].course.courseTitle").value("커스텀강의"))
    }

    @Test
    fun `POST enrolls custom - 기존 강의와 시간 겹치면 409`() {
        val (user, token) = dataGenerator.generateUserWithAccessToken()
        val tt = dataGenerator.generateTimetable(user = user, year = 2025, semester = Semester.FALL)

        // 첫 강의: MON 09:00~10:15
        mockMvc.perform(
            post("/api/v1/timetables/${tt.id}/enrolls/custom")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    toJson(
                        mapOf(
                            "year" to 2025,
                            "semester" to "FALL",
                            "courseTitle" to "A",
                            "timeSlots" to listOf(
                                mapOf("dayOfWeek" to "MON", "startAt" to 540, "endAt" to 615)
                            )
                        )
                    )
                )
        )
            .andExpect(status().isCreated)

        // 두 번째 강의: MON 10:00~11:00 (겹침: 600~615 구간)
        mockMvc.perform(
            post("/api/v1/timetables/${tt.id}/enrolls/custom")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    toJson(
                        mapOf(
                            "year" to 2025,
                            "semester" to "FALL",
                            "courseTitle" to "B",
                            "timeSlots" to listOf(
                                mapOf("dayOfWeek" to "MON", "startAt" to 600, "endAt" to 660)
                            )
                        )
                    )
                )
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `POST enrolls custom - 기존 강의와 시간 안겹치면 201`() {
        val (user, token) = dataGenerator.generateUserWithAccessToken()
        val tt = dataGenerator.generateTimetable(user = user, year = 2025, semester = Semester.FALL)

        // 첫 강의: MON 09:00~10:15
        mockMvc.perform(
            post("/api/v1/timetables/${tt.id}/enrolls/custom")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    toJson(
                        mapOf(
                            "year" to 2025,
                            "semester" to "FALL",
                            "courseTitle" to "A",
                            "timeSlots" to listOf(
                                mapOf("dayOfWeek" to "MON", "startAt" to 540, "endAt" to 615)
                            )
                        )
                    )
                )
        )
            .andExpect(status().isCreated)

        // 두 번째 강의: MON 10:15~11:30 (끝점 맞닿음 = 허용)
        mockMvc.perform(
            post("/api/v1/timetables/${tt.id}/enrolls/custom")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    toJson(
                        mapOf(
                            "year" to 2025,
                            "semester" to "FALL",
                            "courseTitle" to "B",
                            "timeSlots" to listOf(
                                mapOf("dayOfWeek" to "MON", "startAt" to 615, "endAt" to 690)
                            )
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `POST enrolls custom - courseTitle blank면 400`() {
        val (user, token) = dataGenerator.generateUserWithAccessToken()
        val tt = dataGenerator.generateTimetable(user = user, year = 2025, semester = Semester.FALL)

        val req = mapOf(
            "year" to 2025,
            "semester" to "FALL",
            "courseTitle" to "   ", // blank
            "timeSlots" to listOf(
                mapOf("dayOfWeek" to "MON", "startAt" to 540, "endAt" to 615),
            ),
        )

        mockMvc.perform(
            post("/api/v1/timetables/${tt.id}/enrolls/custom")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(req))
        )
            .andExpect(status().isBadRequest)
    }


    @Test
    fun `PATCH enroll - body가 비어있으면 400`() {
        val (user, token) = dataGenerator.generateUserWithAccessToken()
        val tt = dataGenerator.generateTimetable(user = user, year = 2025, semester = Semester.FALL)

        // 먼저 커스텀 enroll 생성
        val createReq = mapOf(
            "year" to 2025,
            "semester" to "FALL",
            "courseTitle" to "커스텀강의",
            "timeSlots" to listOf(
                mapOf("dayOfWeek" to "MON", "startAt" to 540, "endAt" to 615),
            ),
        )

        val created = mockMvc.perform(
            post("/api/v1/timetables/${tt.id}/enrolls/custom")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(createReq))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val enrollId = objectMapper.readTree(created.response.contentAsString)["enrollId"].asLong()

        // PATCH empty body -> ENROLL_PATCH_EMPTY
        mockMvc.perform(
            patch("/api/v1/timetables/${tt.id}/enrolls/$enrollId")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PATCH enroll - courseTitle null이면 400`() {
        val (user, token) = dataGenerator.generateUserWithAccessToken()
        val tt = dataGenerator.generateTimetable(user = user, year = 2025, semester = Semester.FALL)

        val created = mockMvc.perform(
            post("/api/v1/timetables/${tt.id}/enrolls/custom")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    toJson(
                        mapOf(
                            "year" to 2025,
                            "semester" to "FALL",
                            "courseTitle" to "커스텀강의",
                            "timeSlots" to listOf(
                                mapOf("dayOfWeek" to "MON", "startAt" to 540, "endAt" to 615)
                            ),
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andReturn()

        val enrollId = objectMapper.readTree(created.response.contentAsString)["enrollId"].asLong()

        mockMvc.perform(
            patch("/api/v1/timetables/${tt.id}/enrolls/$enrollId")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"courseTitle": null}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PATCH enroll - timeSlots를 다른 강의와 겹치게 수정하면 409`() {
        val (user, token) = dataGenerator.generateUserWithAccessToken()
        val tt = dataGenerator.generateTimetable(user = user, year = 2025, semester = Semester.FALL)

        // 강의 A: MON 09:00~10:15
        val createdA = mockMvc.perform(
            post("/api/v1/timetables/${tt.id}/enrolls/custom")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    toJson(
                        mapOf(
                            "year" to 2025,
                            "semester" to "FALL",
                            "courseTitle" to "A",
                            "timeSlots" to listOf(
                                mapOf("dayOfWeek" to "MON", "startAt" to 540, "endAt" to 615)
                            ),
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andReturn()

        // 강의 B: TUE 09:00~10:15 (처음엔 안 겹치게)
        val createdB = mockMvc.perform(
            post("/api/v1/timetables/${tt.id}/enrolls/custom")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    toJson(
                        mapOf(
                            "year" to 2025,
                            "semester" to "FALL",
                            "courseTitle" to "B",
                            "timeSlots" to listOf(
                                mapOf("dayOfWeek" to "TUE", "startAt" to 540, "endAt" to 615)
                            ),
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andReturn()

        val enrollIdB = objectMapper.readTree(createdB.response.contentAsString)["enrollId"].asLong()

        // B의 timeSlots를 MON 10:00~11:00 으로 수정 → A(MON 09:00~10:15)와 겹침
        val patchBody = mapOf(
            "timeSlots" to listOf(
                mapOf("dayOfWeek" to "MON", "startAt" to 600, "endAt" to 660)
            )
        )

        mockMvc.perform(
            patch("/api/v1/timetables/${tt.id}/enrolls/$enrollIdB")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(patchBody))
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `PATCH enroll - timeSlots를 기존과 동일하게 보내도 200`() {
        val (user, token) = dataGenerator.generateUserWithAccessToken()
        val tt = dataGenerator.generateTimetable(user = user, year = 2025, semester = Semester.FALL)

        val created = mockMvc.perform(
            post("/api/v1/timetables/${tt.id}/enrolls/custom")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    toJson(
                        mapOf(
                            "year" to 2025,
                            "semester" to "FALL",
                            "courseTitle" to "A",
                            "timeSlots" to listOf(
                                mapOf("dayOfWeek" to "MON", "startAt" to 540, "endAt" to 615)
                            ),
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andReturn()

        val enrollId = objectMapper.readTree(created.response.contentAsString)["enrollId"].asLong()

        // 같은 timeSlots로 다시 PATCH
        mockMvc.perform(
            patch("/api/v1/timetables/${tt.id}/enrolls/$enrollId")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    toJson(
                        mapOf(
                            "timeSlots" to listOf(
                                mapOf("dayOfWeek" to "MON", "startAt" to 540, "endAt" to 615)
                            )
                        )
                    )
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.enrollId").value(enrollId))
            .andExpect(jsonPath("$.course.courseTitle").value("A"))
    }

    @Test
    fun `DELETE enroll - 삭제 204, 없으면 404`() {
        val (user, token) = dataGenerator.generateUserWithAccessToken()
        val tt = dataGenerator.generateTimetable(user = user, year = 2025, semester = Semester.FALL)

        val created = mockMvc.perform(
            post("/api/v1/timetables/${tt.id}/enrolls/custom")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    toJson(
                        mapOf(
                            "year" to 2025,
                            "semester" to "FALL",
                            "courseTitle" to "커스텀강의",
                            "timeSlots" to listOf(
                                mapOf("dayOfWeek" to "MON", "startAt" to 540, "endAt" to 615)
                            ),
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andReturn()

        val enrollId = objectMapper.readTree(created.response.contentAsString)["enrollId"].asLong()

        mockMvc.perform(
            delete("/api/v1/timetables/${tt.id}/enrolls/$enrollId")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isNoContent)

        // 다시 삭제하면 404
        mockMvc.perform(
            delete("/api/v1/timetables/${tt.id}/enrolls/$enrollId")
                .header("Authorization", bearer(token))
        )
            .andExpect(status().isNotFound)
    }
}