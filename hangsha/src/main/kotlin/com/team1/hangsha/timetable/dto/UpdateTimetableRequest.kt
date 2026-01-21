package com.team1.hangsha.timetable.dto

import jakarta.validation.constraints.NotBlank

data class UpdateTimetableRequest(
    @field:NotBlank
    val name: String
)