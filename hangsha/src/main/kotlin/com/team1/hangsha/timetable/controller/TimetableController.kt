package com.team1.hangsha.timetable.controller

import com.team1.hangsha.common.enums.Semester
import com.team1.hangsha.timetable.dto.CreateTimetableRequest
import com.team1.hangsha.timetable.dto.ListTimetablesResponse
import com.team1.hangsha.timetable.dto.TimetableResponse
import com.team1.hangsha.timetable.dto.UpdateTimetableRequest
import com.team1.hangsha.timetable.service.TimetableService
import com.team1.hangsha.user.LoggedInUser
import com.team1.hangsha.user.model.User
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/timetables")
class TimetableController(
    private val timetableService: TimetableService,
) {

    // GET /timetables
    @GetMapping
    fun listTimetables(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) semester: Semester?,
    ): ResponseEntity<ListTimetablesResponse> {
        val res = timetableService.listTimetables(
            userId = user.id!!,
            year = year,
            semester = semester
        )
        return ResponseEntity.ok(res)
    }

    // POST /timetables
    @PostMapping
    fun createTimetable(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @RequestBody req: CreateTimetableRequest,
    ): ResponseEntity<TimetableResponse> {
        val res = timetableService.createTimetable(user.id!!, req)
        return ResponseEntity.status(201).body(res)
    }

    // PATCH /timetables/{timetableId}
    @PatchMapping("/{timetableId}")
    fun updateTimetable(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable timetableId: Long,
        @RequestBody req: UpdateTimetableRequest,
    ): ResponseEntity<TimetableResponse> {
        val res = timetableService.updateTimetable(user.id!!, timetableId, req)
        return ResponseEntity.ok(res)
    }

    // DELETE /timetables/{timetableId}
    @DeleteMapping("/{timetableId}")
    fun deleteTimetable(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable timetableId: Long,
    ): ResponseEntity<Void> {
        timetableService.deleteTimetable(user.id!!, timetableId)
        return ResponseEntity.noContent().build()
    }
}