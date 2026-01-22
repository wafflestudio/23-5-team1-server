package com.team1.hangsha.timetable.dto

import jakarta.validation.Valid
import com.team1.hangsha.course.dto.core.CourseTimeSlotDto

data class UpdateCustomCourseRequest(
    val courseTitle: String? = null,

    @field:Valid
    val timeSlots: List<CourseTimeSlotDto>? = null,

    // optional (null로 지우기 허용)
    val courseNumber: String? = null,
    val lectureNumber: String? = null,
    val credit: Int? = null,
    val instructor: String? = null,
)