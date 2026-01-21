package com.team1.hangsha.timetable.dto

import com.team1.hangsha.course.dto.core.CourseDto

data class EnrollResponse(
    val enrollId: Long,
    val course: CourseDto,
)