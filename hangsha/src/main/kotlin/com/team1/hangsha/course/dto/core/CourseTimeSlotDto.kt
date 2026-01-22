package com.team1.hangsha.course.dto.core

import com.team1.hangsha.common.enums.DayOfWeek
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max
import com.team1.hangsha.course.validation.ValidTimeSlot

@ValidTimeSlot
data class CourseTimeSlotDto(
    @field:NotNull
    val dayOfWeek: DayOfWeek,

    @field:Min(0)
    @field:Max(1439)
    val startAt: Int,

    @field:Min(1)
    @field:Max(1440)
    val endAt: Int,
)