package com.team1.hangsha.event.service

import com.team1.hangsha.common.error.DomainException
import com.team1.hangsha.common.error.ErrorCode
import com.team1.hangsha.event.dto.core.EventDto
import com.team1.hangsha.event.dto.response.Calendar.MonthEventResponse
import com.team1.hangsha.event.dto.response.DetailEventResponse
import com.team1.hangsha.event.dto.response.Calendar.DayEventResponse
import com.team1.hangsha.event.dto.response.TitleSearchEventResponse
import com.team1.hangsha.event.model.Event
import com.team1.hangsha.event.repository.EventQueryRepository
import com.team1.hangsha.event.repository.EventRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import kotlin.math.max

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val eventQueryRepository: EventQueryRepository,
) {
    fun getMonthEvents(
        from: LocalDate,
        to: LocalDate,
        statusIds: List<Long>?,
        eventTypeIds: List<Long>?,
        orgIds: List<Long>?,
        previewSize: Int = 3,
    ): MonthEventResponse {
        if (from.isAfter(to)) {
            throw DomainException(ErrorCode.INVALID_REQUEST, "from은 to보다 이후일 수 없습니다")
        }

        val fromStart = from.atStartOfDay()
        val toEndExclusive = to.plusDays(1).atStartOfDay()

        val events = eventQueryRepository.findInRange(
            fromStart = fromStart,
            toEndExclusive = toEndExclusive,
            statusIds = statusIds,
            eventTypeIds = eventTypeIds,
            orgIds = orgIds,
        )

        val grouped = events.groupBy { e ->
            (e.eventStart ?: e.applyStart ?: fromStart).toLocalDate()
        }.toSortedMap()

        val byDate = grouped
            .mapValues { (_, dayEvents) ->
                MonthEventResponse.DayBucket(
                    total = dayEvents.size,
                    preview = dayEvents.take(previewSize).map { it.toDto() },
                )
            }
            .mapKeys { (date, _) -> date.toString() }

        return MonthEventResponse(
            range = MonthEventResponse.Range(from = from, to = to),
            byDate = byDate,
        )
    }

    fun getEventDetail(eventId: Long): DetailEventResponse {
        val event = eventRepository.findById(eventId).orElseThrow {
            DomainException(ErrorCode.EVENT_NOT_FOUND)
        }
        return event.toDetailResponse()
    }

    fun getDayEvents(
        date: LocalDate,
        page: Int,
        size: Int,
        statusIds: List<Long>?,
        eventTypeIds: List<Long>?,
        orgIds: List<Long>?,
    ): DayEventResponse {
        val safePage = max(1, page)
        val safeSize = max(1, size)
        val offset = (safePage - 1) * safeSize

        val fromStart = date.atStartOfDay()
        val toEndExclusive = date.plusDays(1).atStartOfDay()

        val total = eventQueryRepository.countInRange(
            fromStart = fromStart,
            toEndExclusive = toEndExclusive,
            statusIds = statusIds,
            eventTypeIds = eventTypeIds,
            orgIds = orgIds,
        )

        val items = eventQueryRepository.findInRangePaged(
            fromStart = fromStart,
            toEndExclusive = toEndExclusive,
            statusIds = statusIds,
            eventTypeIds = eventTypeIds,
            orgIds = orgIds,
            offset = offset,
            limit = safeSize,
        ).map { it.toDto() }

        return DayEventResponse(
            page = safePage,
            size = safeSize,
            total = total,
            date = date,
            items = items,
        )
    }

    fun searchTitle(
        query: String,
        page: Int,
        size: Int,
    ): TitleSearchEventResponse {
        val q = query.trim()
        if (q.isEmpty()) {
            throw DomainException(ErrorCode.INVALID_REQUEST, "query는 비어있을 수 없습니다")
        }

        val safePage = max(1, page)
        val safeSize = max(1, size)
        val offset = (safePage - 1) * safeSize

        val total = eventQueryRepository.countByTitleContains(q)
        val items = eventQueryRepository.findByTitleContainsPaged(q, offset, safeSize)
            .map { it.toDto() }

        return TitleSearchEventResponse(
            page = safePage,
            size = safeSize,
            total = total,
            items = items,
        )
    }
}

private fun Event.toDto(): EventDto = EventDto(
    id = requireNotNull(id),
    title = title,
    imageUrl = imageUrl,
    operationMode = operationMode,
    statusId = statusId,
    eventTypeId = eventTypeId,
    orgId = orgId,
    applyStart = applyStart,
    applyEnd = applyEnd,
    eventStart = eventStart,
    eventEnd = eventEnd,
    capacity = capacity,
    applyCount = applyCount,
    organization = organization,
    location = location,
    applyLink = applyLink,
    isInterested = null,
    matchedInterestPriority = null,
    isBookmarked = null,
    tags = tags,
)

private fun Event.toDetailResponse(): DetailEventResponse = DetailEventResponse(
    id = requireNotNull(id),
    title = title,
    imageUrl = imageUrl,
    operationMode = operationMode,
    statusId = statusId,
    eventTypeId = eventTypeId,
    orgId = orgId,
    applyStart = applyStart,
    applyEnd = applyEnd,
    eventStart = eventStart,
    eventEnd = eventEnd,
    capacity = capacity,
    applyCount = applyCount,
    organization = organization,
    location = location,
    applyLink = applyLink,
    isInterested = null,
    matchedInterestPriority = null,
    isBookmarked = null,
    tags = tags,
    // 현재 events 테이블에 detail 컬럼이 없어서 null
    detail = mainContentHtml,
)
