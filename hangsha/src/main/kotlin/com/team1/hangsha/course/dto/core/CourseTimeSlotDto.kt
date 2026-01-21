package com.team1.hangsha.course.dto.core

import com.team1.hangsha.common.enums.DayOfWeek

data class CourseTimeSlotDto(
    val dayOfWeek: DayOfWeek,
    /** 분 단위 (예: 630 = 10:30) */
    val startAt: Int,
    val endAt: Int,
)