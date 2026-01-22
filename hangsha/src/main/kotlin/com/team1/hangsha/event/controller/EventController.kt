package com.team1.hangsha.event.controller

import com.team1.hangsha.event.dto.response.Calendar.MonthEventResponse
import com.team1.hangsha.event.dto.response.Calendar.DayEventResponse
import com.team1.hangsha.event.dto.response.TitleSearchEventResponse
import com.team1.hangsha.event.dto.response.DetailEventResponse
import com.team1.hangsha.event.service.EventService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/events")
class EventController(
    private val eventService: EventService,
) {

    @GetMapping("/month")
    fun month(
        @RequestParam("from") @DateTimeFormat(iso = ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = ISO.DATE) to: LocalDate,
        @RequestParam("statusId", required = false) statusIds: List<Long>?,
        @RequestParam("eventTypeId", required = false) eventTypeIds: List<Long>?,
        @RequestParam("orgId", required = false) orgIds: List<Long>?,
    ): MonthEventResponse =
        eventService.getMonthEvents(
            from = from,
            to = to,
            statusIds = statusIds,
            eventTypeIds = eventTypeIds,
            orgIds = orgIds,
        )

    @GetMapping("/{eventId}")
    fun detail(
        @PathVariable eventId: Long,
    ): DetailEventResponse =
        eventService.getEventDetail(eventId)

    @GetMapping("/day")
    fun day(
        @RequestParam("date") @DateTimeFormat(iso = ISO.DATE) date: LocalDate,
        @RequestParam("page", defaultValue = "1") page: Int,
        @RequestParam("size", defaultValue = "20") size: Int,
        @RequestParam("statusId", required = false) statusIds: List<Long>?,
        @RequestParam("eventTypeId", required = false) eventTypeIds: List<Long>?,
        @RequestParam("orgId", required = false) orgIds: List<Long>?,
    ): DayEventResponse =
        eventService.getDayEvents(
            date = date,
            page = page,
            size = size,
            statusIds = statusIds,
            eventTypeIds = eventTypeIds,
            orgIds = orgIds,
        )

    @GetMapping("/search/title")
    fun searchTitle(
        @RequestParam("query") query: String,
        @RequestParam("page", defaultValue = "1") page: Int,
        @RequestParam("size", defaultValue = "20") size: Int,
    ): TitleSearchEventResponse =
        eventService.searchTitle(
            query = query,
            page = page,
            size = size,
        )
}
