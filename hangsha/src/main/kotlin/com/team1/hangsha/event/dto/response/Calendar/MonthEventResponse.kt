package com.team1.hangsha.event.dto.response.Calendar

import com.team1.hangsha.event.dto.core.EventDto
import java.time.LocalDate

data class MonthEventResponse(
    val range: Range,
    /**
     * key: yyyy-MM-dd
     */
    val byDate: Map<String, DayBucket>,
) {
    data class Range(
        val from: LocalDate,
        val to: LocalDate,
    )

    data class DayBucket(
        val events: List<EventDto>,
    )
}