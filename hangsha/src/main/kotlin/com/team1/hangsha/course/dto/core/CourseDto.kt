package com.team1.hangsha.course.dto.core

import com.team1.hangsha.common.enums.Semester
import com.team1.hangsha.common.enums.CourseSource

data class CourseDto(
    val id: Long,
    val year: Int,
    val semester: Semester,
    val courseTitle: String,
    val source: CourseSource,
    val timeSlots: List<CourseTimeSlotDto>,

    // optional / nullable
    val courseNumber: String? = null,
    val lectureNumber: String? = null,
    val credit: Int? = null,
    val instructor: String? = null,
)