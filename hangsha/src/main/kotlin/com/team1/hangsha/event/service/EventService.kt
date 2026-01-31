package com.team1.hangsha.event.service

import com.team1.hangsha.bookmark.repository.BookmarkRepository
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
import java.time.LocalDateTime
import kotlin.math.max

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val eventQueryRepository: EventQueryRepository,
    private val bookmarkRepository: BookmarkRepository,
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

        // 날짜별 버킷: 그 날과 겹치면 포함 (day overlap과 동일한 개념)
        val buckets = linkedMapOf<LocalDate, MutableList<Event>>().apply {
            var d = from
            while (!d.isAfter(to)) {
                this[d] = mutableListOf()
                d = d.plusDays(1)
            }
        }

        fun effectiveStart(e: Event): LocalDateTime =
            e.eventStart ?: e.applyStart ?: fromStart

        fun effectiveEnd(e: Event): LocalDateTime =
            e.eventEnd ?: e.applyEnd ?: effectiveStart(e)

        for (e in events) {
            val s = effectiveStart(e).toLocalDate().coerceAtLeast(from)
            val ee = effectiveEnd(e).toLocalDate().coerceAtMost(to)

            var d = s
            while (!d.isAfter(ee)) {
                buckets[d]?.add(e)
                d = d.plusDays(1)
            }
        }

        val byDate = buckets
            .filterValues { it.isNotEmpty() }
            .toSortedMap()
            .mapValues { (_, dayEvents) ->
                val sorted = dayEvents.sortedWith(
                    compareBy<Event> { effectiveStart(it) }.thenBy { it.id ?: Long.MAX_VALUE }
                )
                MonthEventResponse.DayBucket(
                    total = sorted.size,
                    preview = sorted.take(previewSize).map { it.toDto() },
                )
            }
            .mapKeys { (date, _) -> date.toString() }

        return MonthEventResponse(
            range = MonthEventResponse.Range(from = from, to = to),
            byDate = byDate,
        )
    }

    fun getEventDetail(eventId: Long, userId: Long?): DetailEventResponse {
        val event = eventRepository.findById(eventId).orElseThrow {
            DomainException(ErrorCode.EVENT_NOT_FOUND)
        }

        val isBookmarked: Boolean? = userId?.let { uid ->
            bookmarkRepository.exists(uid, eventId)
        }

        return event.toDetailResponse(isBookmarked)
    }

    // 기존 코드 호환용
    fun getEventDetail(eventId: Long): DetailEventResponse =
        getEventDetail(eventId, null)

    fun getDayEvents(
        date: LocalDate,
        page: Int,
        size: Int,
        statusIds: List<Long>?,
        eventTypeIds: List<Long>?,
        orgIds: List<Long>?,
    ): DayEventResponse {
        val total = eventQueryRepository.countOnDay(date, statusIds, eventTypeIds, orgIds)
        val items = eventQueryRepository.findOnDayPaged(date, statusIds, eventTypeIds, orgIds, page, size)
            .map { it.toDto() }

        return DayEventResponse(
            page = max(1, page),
            size = max(1, size),
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

private fun Event.toDetailResponse(isBookmarked: Boolean?): DetailEventResponse = DetailEventResponse(
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
    isBookmarked = isBookmarked,
    tags = tags,
    detail = mainContentHtml,
)
