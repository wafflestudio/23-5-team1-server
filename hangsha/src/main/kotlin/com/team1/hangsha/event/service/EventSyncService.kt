package com.team1.hangsha.event.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.team1.hangsha.category.model.Category
import com.team1.hangsha.category.repository.CategoryGroupRepository
import com.team1.hangsha.category.repository.CategoryRepository
import com.team1.hangsha.common.error.DomainException
import com.team1.hangsha.common.error.ErrorCode
import com.team1.hangsha.event.model.Event
import com.team1.hangsha.event.repository.EventRepository
import com.team1.hangsha.event.dto.core.CrawledDetailSession
import com.team1.hangsha.event.dto.core.CrawledProgramEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Instant

@Service
class EventSyncService(
    private val objectMapper: ObjectMapper,
    private val eventRepository: EventRepository,
    private val categoryGroupRepository: CategoryGroupRepository,
    private val categoryRepository: CategoryRepository,
    @Value("\${crawler.events-json-path}") private val eventsJsonPath: String,
) {

    data class SyncResult(val total: Int, val upserted: Int, val skipped: Int)

    @Transactional
    fun syncFromFile(): SyncResult = syncFromFile(Path.of(eventsJsonPath))

    @Transactional
    fun syncFromFile(path: Path): SyncResult {
        require(Files.exists(path)) { "events.json not found: $path" }

        val events: List<CrawledProgramEvent> =
            objectMapper.readValue(
                Files.readString(path),
                objectMapper.typeFactory.constructCollectionType(List::class.java, CrawledProgramEvent::class.java)
            )

        val statusGroupId = requireGroupId("모집현황")
        val typeGroupId = requireGroupId("프로그램 유형")
        val orgGroupId = requireGroupId("주체기관")

        // 혹시 몰라서, sync 결과 추적을 위한 변수들
        var upserted = 0
        var skipped = 0

        for (e in events) {
            val applyLink = "https://extra.snu.ac.kr/ptfol/pgm/view.do?dataSeq=${e.dataSeq}"

            val orgName = e.majorTypes.getOrNull(0)?.trim()?.takeIf { it.isNotBlank() }
            val orgId = orgName?.let { getOrCreateCategoryId(orgGroupId, it) }
            val typeName = normalizeProgramType(e.majorTypes.getOrNull(1))

            val statusId = e.status?.trim()?.takeIf { it.isNotBlank() }?.let { findCategoryId(statusGroupId, it) }
            val eventTypeId = typeName?.let { findCategoryId(typeGroupId, it) }

            val applyStart = e.applyStart?.let { dateStart(it) }
            val applyEnd = e.applyEnd?.let { dateEnd(it) }

            // 상세 디테일에서 와야 하니까, 따로 함수 설정
            val sessions = patchSessionTimesFromMainContent(e.detailSessions, e.mainContentHtml)

            // (1) 저장할 “단위들”만 만든다. (세션 여러 개면 세션별, 아니면 기존 집계 1개)
            data class UnitSpec(
                val eventStart: LocalDateTime?,
                val eventEnd: LocalDateTime?,
                val location: String?,
            )

            val unitSpecs: List<UnitSpec> =
                if (sessions.size >= 2) {
                    sessions.map { s ->
                        UnitSpec(
                            eventStart = parseSessionStart(s),
                            eventEnd = parseSessionEnd(s),
                            location = s.location?.trim()?.takeIf { it.isNotBlank() }
                        )
                    }
                } else {
                    val (eventStart, eventEnd, location) = deriveEventPeriodAndLocation(e, sessions)
                    listOf(UnitSpec(eventStart, eventEnd, location?.trim()?.takeIf { it.isNotBlank() }))
                }

            // (2) 저장 로직은 완전히 동일: 한 루프에서 업서트 후 save
            for (spec in unitSpecs) {
                val eventStart = spec.eventStart
                val eventEnd = spec.eventEnd
                val location = spec.location

                // 업서트 키: COALESCE(eventStart, applyStart) / COALESCE(eventEnd, applyEnd)
                val keyStart = eventStart ?: applyStart
                val keyEnd = eventEnd ?: applyEnd

                // keyStart/keyEnd 둘 중 하나라도 null이면 업서트 기준이 깨지므로 insert만(원하면 정책 바꿔도 됨)
                val existing =
                    if (keyStart != null && keyEnd != null) {
                        eventRepository.findLatestByApplyLinkAndKeyPeriod(
                            applyLink = applyLink,
                            keyStart = keyStart,
                            keyEnd = keyEnd,
                        )
                    } else null

                val cleanedTags = e.tags
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .toList()

                val model = Event(
                    id = existing?.id,
                    title = e.title!!.trim(),
                    imageUrl = e.imageUrl?.trim(),
                    operationMode = e.operationMode?.trim(),

                    statusId = statusId,
                    eventTypeId = eventTypeId,
                    orgId = orgId,
                    applyStart = applyStart,
                    applyEnd = applyEnd,
                    eventStart = eventStart,
                    eventEnd = eventEnd,

                    capacity = e.capacity ?: 0,
                    applyCount = e.applyCount ?: 0,

                    organization = orgName,
                    location = location,
                    applyLink = applyLink,

                    tags = if (cleanedTags.isEmpty()) null else objectMapper.writeValueAsString(cleanedTags),
                    mainContentHtml = e.mainContentHtml,

                    createdAt = existing?.createdAt ?: Instant.now(),
                )

                eventRepository.save(model)
                upserted++
            }
        }

        return SyncResult(total = events.size, upserted = upserted, skipped = skipped)
    }

    private fun requireGroupId(name: String): Long {
        val group = categoryGroupRepository.findByName(name)
            ?: throw DomainException(ErrorCode.CATEGORY_GROUP_NOT_FOUND)

        return group.id
            ?: throw DomainException(
                ErrorCode.INTERNAL_ERROR,
                "CategoryGroup.id is null (unexpected). name=$name"
            )
    }

    private fun findCategoryId(groupId: Long, name: String): Long? =
        categoryRepository.findByGroupIdAndName(groupId, name)?.id

    private fun deriveEventPeriodAndLocation(
        e: CrawledProgramEvent,
        sessions: List<CrawledDetailSession>
    ): Triple<LocalDateTime?, LocalDateTime?, String?> {
        if (sessions.isNotEmpty()) {
            val starts = sessions.mapNotNull { parseSessionStart(it) }
            val ends = sessions.mapNotNull { parseSessionEnd(it) }
            val start = starts.minOrNull()
            val end = ends.maxOrNull()
            val location = sessions.firstNotNullOfOrNull { it.location?.trim()?.takeIf { s -> s.isNotBlank() } }
            return Triple(start, end, location)
        }

        val start = e.activityStart?.let { LocalDate.parse(it).atStartOfDay() }
        val end = e.activityEnd?.let { LocalDate.parse(it).atTime(23, 59, 59) }
        return Triple(start, end, null)
    }

    private fun parseSessionStart(s: CrawledDetailSession): LocalDateTime? {
        val d = s.startDate ?: return null
        val date = LocalDate.parse(d)
        val time = s.startTime?.let { LocalTime.parse(it) } ?: LocalTime.MIDNIGHT
        return date.atTime(time)
    }

    private fun parseSessionEnd(s: CrawledDetailSession): LocalDateTime? {
        val d = s.endDate ?: s.startDate ?: return null
        val date = LocalDate.parse(d)
        val time = s.endTime?.let { LocalTime.parse(it) } ?: LocalTime.of(23, 59, 59)
        return date.atTime(time)
    }

    private fun dateStart(ymd: String): LocalDateTime =
        LocalDate.parse(ymd).atStartOfDay()

    private fun dateEnd(ymd: String): LocalDateTime =
        LocalDate.parse(ymd).atTime(23, 59, 59)

    private fun getOrCreateCategoryId(groupId: Long, rawName: String): Long {
        val name = rawName.trim()
        categoryRepository.findByGroupIdAndName(groupId, name)?.id?.let { return it }

        val nextSortOrder = runCatching { categoryRepository.findMaxSortOrderByGroupId(groupId) + 1 }
            .getOrDefault(1)

        return try {
            val saved = categoryRepository.save(
                Category(
                    groupId = groupId,
                    name = name,
                    sortOrder = nextSortOrder
                )
            )
            saved.id ?: throw DomainException(ErrorCode.CATEGORY_CREATE_FAILED)
        } catch (e: DuplicateKeyException) {
            // 동시에 누가 먼저 insert 했을 수 있음 → 재조회
            categoryRepository.findByGroupIdAndName(groupId, name)?.id ?: throw e
        }
    }

    private fun normalizeProgramType(raw: String?): String? {
        val s = raw?.trim()
        if (s.isNullOrBlank()) return null
        return when (s) {
            "레크리에이션" -> "기타"
            else -> s
        }
    }

    // mainContentHtml에서 "HH:mm ~ HH:mm" 찾기 (공백 유무 허용)
    private val timeRangeRegex = Regex("""\b(\d{1,2}:\d{2})\s*~\s*(\d{1,2}:\d{2})\b""")

    private fun normalizeHm(raw: String): String? {
        val m = Regex("""^(\d{1,2}):(\d{2})$""").find(raw.trim()) ?: return null
        val h = m.groupValues[1].toIntOrNull() ?: return null
        val mi = m.groupValues[2].toIntOrNull() ?: return null
        if (h !in 0..23 || mi !in 0..59) return null
        return "%02d:%02d".format(h, mi)
    }

    /**
     * mainContentHtml에 시간 범위가 있으면, startTime/endTime이 비어있는 세션들에만 채움.
     * (없으면 기존 값 그대로)
     */
    private fun patchSessionTimesFromMainContent(
        sessions: List<CrawledDetailSession>,
        mainContentHtml: String?
    ): List<CrawledDetailSession> {
        if (sessions.isEmpty()) return sessions
        if (mainContentHtml.isNullOrBlank()) return sessions
        if (sessions.none { it.startTime == null && it.endTime == null }) return sessions

        val m = timeRangeRegex.find(mainContentHtml) ?: return sessions
        val start = normalizeHm(m.groupValues[1]) ?: return sessions
        val end = normalizeHm(m.groupValues[2]) ?: return sessions

        return sessions.map { s ->
            if (s.startTime == null && s.endTime == null) s.copy(startTime = start, endTime = end) else s
        }
    }
}
