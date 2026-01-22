package com.team1.hangsha.timetable.dto

import com.team1.hangsha.common.enums.Semester

data class TimetableResponse(
    val id: Long,
    val name: String,
    val year: Int,
    val semester: Semester,
)