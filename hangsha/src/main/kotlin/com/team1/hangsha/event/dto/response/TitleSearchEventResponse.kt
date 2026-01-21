package com.team1.hangsha.event.dto.response

import com.team1.hangsha.event.dto.core.EventDto

data class TitleSearchEventResponse(
    val page: Int,
    val size: Int,
    val total: Int,
    val items: List<EventDto>,
)
