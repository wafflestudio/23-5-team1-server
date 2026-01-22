package com.team1.hangsha.event.repository

import com.team1.hangsha.event.model.Event
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.max

@Repository
class EventQueryRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {
    fun findInRange(
        fromStart: LocalDateTime,
        toEndExclusive: LocalDateTime,
        statusIds: List<Long>?,
        eventTypeIds: List<Long>?,
        orgIds: List<Long>?,
    ): List<Event> {
        val sql = buildString {
            append(
                """
                SELECT *
                FROM events
                WHERE COALESCE(event_start, apply_start) >= :fromStart
                  AND COALESCE(event_start, apply_start) < :toEndExclusive
                """.trimIndent()
            )

            if (!statusIds.isNullOrEmpty()) append("\n  AND status_id IN (:statusIds)")
            if (!eventTypeIds.isNullOrEmpty()) append("\n  AND event_type_id IN (:eventTypeIds)")
            if (!orgIds.isNullOrEmpty()) append("\n  AND org_id IN (:orgIds)")

            append("\nORDER BY COALESCE(event_start, apply_start) ASC, id ASC")
        }

        val params = mutableMapOf<String, Any>(
            "fromStart" to Timestamp.valueOf(fromStart),
            "toEndExclusive" to Timestamp.valueOf(toEndExclusive),
        )
        if (!statusIds.isNullOrEmpty()) params["statusIds"] = statusIds
        if (!eventTypeIds.isNullOrEmpty()) params["eventTypeIds"] = eventTypeIds
        if (!orgIds.isNullOrEmpty()) params["orgIds"] = orgIds

        return jdbc.query(sql, params) { rs, _ -> rs.toEvent() }
    }

    fun countOnDay(
        date: LocalDate,
        statusIds: List<Long>?,
        eventTypeIds: List<Long>?,
        orgIds: List<Long>?,
    ): Int {
        val dayStart = date.atStartOfDay()
        val dayEndExclusive = date.plusDays(1).atStartOfDay()

        val sql = buildString {
            append(
                """
            SELECT COUNT(*)
            FROM events
            WHERE (
              (event_start IS NOT NULL AND event_start < :dayEnd AND event_end >= :dayStart)
              OR
              (event_start IS NULL AND apply_start < :dayEnd AND apply_end >= :dayStart)
            )
            """.trimIndent()
            )
            if (!statusIds.isNullOrEmpty()) append("\n  AND status_id IN (:statusIds)")
            if (!eventTypeIds.isNullOrEmpty()) append("\n  AND event_type_id IN (:eventTypeIds)")
            if (!orgIds.isNullOrEmpty()) append("\n  AND org_id IN (:orgIds)")
        }

        val params = mutableMapOf<String, Any>(
            "dayStart" to Timestamp.valueOf(dayStart),
            "dayEnd" to Timestamp.valueOf(dayEndExclusive),
        )
        if (!statusIds.isNullOrEmpty()) params["statusIds"] = statusIds
        if (!eventTypeIds.isNullOrEmpty()) params["eventTypeIds"] = eventTypeIds
        if (!orgIds.isNullOrEmpty()) params["orgIds"] = orgIds

        return jdbc.queryForObject(sql, params, Int::class.java) ?: 0
    }

    fun findOnDayPaged(
        date: LocalDate,
        statusIds: List<Long>?,
        eventTypeIds: List<Long>?,
        orgIds: List<Long>?,
        page: Int,
        size: Int,
    ): List<Event> {
        val safePage = max(1, page)
        val safeSize = max(1, size)
        val offset = (safePage - 1) * safeSize

        val dayStart = date.atStartOfDay()
        val dayEndExclusive = date.plusDays(1).atStartOfDay()

        val sql = buildString {
            append(
                """
            SELECT *
            FROM events
            WHERE (
              (event_start IS NOT NULL AND event_start < :dayEnd AND event_end >= :dayStart)
              OR
              (event_start IS NULL AND apply_start < :dayEnd AND apply_end >= :dayStart)
            )
            """.trimIndent()
            )
            if (!statusIds.isNullOrEmpty()) append("\n  AND status_id IN (:statusIds)")
            if (!eventTypeIds.isNullOrEmpty()) append("\n  AND event_type_id IN (:eventTypeIds)")
            if (!orgIds.isNullOrEmpty()) append("\n  AND org_id IN (:orgIds)")

            append("\nORDER BY COALESCE(event_start, apply_start) DESC, id DESC")
            append("\nLIMIT :limit OFFSET :offset")
        }

        val params = mutableMapOf<String, Any>(
            "dayStart" to Timestamp.valueOf(dayStart),
            "dayEnd" to Timestamp.valueOf(dayEndExclusive),
            "limit" to safeSize,
            "offset" to offset,
        )
        if (!statusIds.isNullOrEmpty()) params["statusIds"] = statusIds
        if (!eventTypeIds.isNullOrEmpty()) params["eventTypeIds"] = eventTypeIds
        if (!orgIds.isNullOrEmpty()) params["orgIds"] = orgIds

        return jdbc.query(sql, params) { rs, _ -> rs.toEvent() }
    }

    fun countByTitleContains(query: String): Int {
        val sql = """
            SELECT COUNT(*)
            FROM events
            WHERE title LIKE :q
        """.trimIndent()

        val params = mapOf("q" to "%$query%")
        return jdbc.queryForObject(sql, params, Int::class.java) ?: 0
    }

    fun findByTitleContainsPaged(query: String, offset: Int, limit: Int): List<Event> {
        val sql = """
            SELECT *
            FROM events
            WHERE title LIKE :q
            ORDER BY COALESCE(event_start, apply_start) DESC, id DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = mapOf(
            "q" to "%$query%",
            "limit" to max(0, limit),
            "offset" to max(0, offset),
        )

        return jdbc.query(sql, params) { rs, _ -> rs.toEvent() }
    }
}

private fun ResultSet.getLocalDateTimeOrNull(column: String): LocalDateTime? =
    getTimestamp(column)?.toLocalDateTime()

private fun ResultSet.getInstantOrNull(column: String): java.time.Instant? =
    getTimestamp(column)?.toInstant()

private fun ResultSet.toEvent(): Event {
    return Event(
        id = getLong("id").let { if (wasNull()) null else it },
        title = getString("title"),
        imageUrl = getString("image_url"),
        operationMode = getString("operation_mode"),

        statusId = getLong("status_id").let { if (wasNull()) null else it },
        eventTypeId = getLong("event_type_id").let { if (wasNull()) null else it },
        orgId = getLong("org_id").let { if (wasNull()) null else it },

        applyStart = getLocalDateTimeOrNull("apply_start"),
        applyEnd = getLocalDateTimeOrNull("apply_end"),
        eventStart = getLocalDateTimeOrNull("event_start"),
        eventEnd = getLocalDateTimeOrNull("event_end"),

        capacity = getInt("capacity").let { if (wasNull()) null else it },
        applyCount = getInt("apply_count"),

        organization = getString("organization"),
        location = getString("location"),
        applyLink = getString("apply_link"),

        createdAt = getInstantOrNull("created_at"),
        updatedAt = getInstantOrNull("updated_at"),
    )
}
