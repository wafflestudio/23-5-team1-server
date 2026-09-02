package com.team1.hangsha.notification.repository

import com.team1.hangsha.notification.model.BookmarkNotificationTarget
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * 북마크 알림 대상만 조회합니다.
 * 실제 기기/FCM 발송은 outbox dispatcher 단계에서 처리합니다.
 */
@Repository
class BookmarkNotificationQueryRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {
    fun findApplyDeadlineTargets(today: LocalDate): List<BookmarkNotificationTarget> =
        findTargets(
            condition = "e.apply_end >= :from AND e.apply_end < :to",
            params = dateRange(today),
        )

    fun findEventStartTargets(tomorrow: LocalDate): List<BookmarkNotificationTarget> =
        findTargets(
            condition = """
                e.event_start >= :from AND e.event_start < :to
                AND (event_type.name IS NULL OR event_type.name <> '공모전/경진대회')
            """.trimIndent(),
            params = dateRange(tomorrow),
        )

    fun findContestDeadlineTargets(tomorrow: LocalDate): List<BookmarkNotificationTarget> =
        findTargets(
            condition = """
                e.event_end >= :from AND e.event_end < :to
                AND event_type.name = '공모전/경진대회'
            """.trimIndent(),
            params = dateRange(tomorrow),
        )

    private fun findTargets(
        condition: String,
        params: Map<String, Any>,
    ): List<BookmarkNotificationTarget> = jdbc.query(
        """
            SELECT b.user_id, e.id AS event_id, e.title AS event_title
            FROM events e
            JOIN bookmarks b ON b.event_id = e.id
            LEFT JOIN categories event_type ON event_type.id = e.event_type_id
            LEFT JOIN notification_preferences preference ON preference.user_id = b.user_id
            WHERE $condition
              AND COALESCE(preference.push_enabled, TRUE) = TRUE
              AND COALESCE(preference.bookmark_event_enabled, TRUE) = TRUE
            ORDER BY e.id, b.user_id
        """.trimIndent(),
        params,
    ) { rs, _ ->
        BookmarkNotificationTarget(
            userId = rs.getLong("user_id"),
            eventId = rs.getLong("event_id"),
            eventTitle = rs.getString("event_title"),
        )
    }

    private fun dateRange(date: LocalDate): Map<String, Any> = mapOf(
        "from" to date.atStartOfDay(),
        "to" to date.plusDays(1).atStartOfDay(),
    )
}
