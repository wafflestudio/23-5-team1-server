package com.team1.hangsha.timetable.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import com.team1.hangsha.common.enums.Semester

data class CreateTimetableRequest(
    @field:NotBlank
    val name: String,

    @field:NotNull
    val year: Int,

    @field:NotNull
    val semester: Semester,
)