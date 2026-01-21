package com.team1.hangsha.event.dto.response.Calendar

import com.team1.hangsha.event.dto.core.EventDto
import java.time.LocalDate

data class DayEventResponse(
    val page: Int,
    val size: Int,
    val total: Int,
    val date: LocalDate,
    val items: List<EventDto>,
)
