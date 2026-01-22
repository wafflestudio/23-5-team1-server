package com.team1.hangsha.timetable.dto

import jakarta.validation.constraints.NotNull

data class AddCourseRequest(
    @field:NotNull
    val courseId: Long
)
