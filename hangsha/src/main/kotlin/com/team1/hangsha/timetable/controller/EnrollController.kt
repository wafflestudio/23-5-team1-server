package com.team1.hangsha.timetable.controller

import com.fasterxml.jackson.databind.JsonNode
import com.team1.hangsha.timetable.dto.AddCourseRequest
import com.team1.hangsha.timetable.dto.AddCourseResponse
import com.team1.hangsha.timetable.dto.CreateCustomCourseRequest
import com.team1.hangsha.timetable.dto.EnrollResponse
import com.team1.hangsha.timetable.dto.ListEnrollsResponse
import com.team1.hangsha.timetable.service.EnrollService
import com.team1.hangsha.user.LoggedInUser
import com.team1.hangsha.user.model.User
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/timetables/{timetableId}/enrolls")
class EnrollController(
    private val enrollService: EnrollService,
) {
    // GET /api/v1/timetables/{timetableId}/enrolls
    @GetMapping
    fun listEnrolls(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable timetableId: Long,
    ): ResponseEntity<ListEnrollsResponse> {
        val res = enrollService.listEnrolls(user.id!!, timetableId)
        return ResponseEntity.ok(res)
    }

    // GET /api/v1/timetables/{timetableId}/enrolls/{enrollId}
    @GetMapping("/{enrollId}")
    fun getEnroll(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable timetableId: Long,
        @PathVariable enrollId: Long,
    ): ResponseEntity<EnrollResponse> {
        val res = enrollService.getEnroll(user.id!!, timetableId, enrollId)
        return ResponseEntity.ok(res)
    }

    // POST /api/v1/timetables/{timetableId}/enrolls/custom
    @PostMapping("/custom")
    fun createCustomCourseAndEnroll(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable timetableId: Long,
        @RequestBody req: CreateCustomCourseRequest,
    ): ResponseEntity<EnrollResponse> {
        val res = enrollService.createCustomCourseAndEnroll(user.id!!, timetableId, req)
        return ResponseEntity.status(HttpStatus.CREATED).body(res)
    }

//    // (추후) POST /api/v1/timetables/{timetableId}/enrolls  (크롤링 강의 추가)
//    @PostMapping
//    fun addCrawledCourse(
//        @Parameter(hidden = true) @LoggedInUser user: User,
//        @PathVariable timetableId: Long,
//        @RequestBody req: AddCourseRequest,
//    ): ResponseEntity<AddCourseResponse> {
//        val res = enrollService.addCrawledCourse(user.id!!, timetableId, req.courseId)
//        return ResponseEntity.status(HttpStatus.CREATED).body(res)
//    }

    // PATCH /api/v1/timetables/{timetableId}/enrolls/{enrollId}
    @PatchMapping("/{enrollId}")
    fun updateCustomEnroll(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable timetableId: Long,
        @PathVariable enrollId: Long,
        @RequestBody body: JsonNode,
    ): ResponseEntity<EnrollResponse> {
        val res = enrollService.updateCustomEnroll(user.id!!, timetableId, enrollId, body)
        return ResponseEntity.ok(res)
    }

    // DELETE /api/v1/timetables/{timetableId}/enrolls/{enrollId}
    @DeleteMapping("/{enrollId}")
    fun deleteEnroll(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable timetableId: Long,
        @PathVariable enrollId: Long,
    ): ResponseEntity<Void> {
        enrollService.deleteEnroll(user.id!!, timetableId, enrollId)
        return ResponseEntity.noContent().build()
    }
}