package com.team1.hangsha.timetable.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import com.team1.hangsha.common.enums.Semester

data class CreateCustomCourseRequest(
    @field:NotNull
    val year: Int,

    @field:NotNull
    val semester: Semester,

    @field:NotBlank
    val courseTitle: String,

    @field:Valid
    @field:Size(min = 1, message = "timeSlots must have at least 1 item")
    val timeSlots: List<CourseTimeSlotDto>,

    // optional
    val courseNumber: String? = null,
    val lectureNumber: String? = null,
    val credit: Int? = null,
    val instructor: String? = null,
)