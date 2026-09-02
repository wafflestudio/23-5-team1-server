package com.team1.hangsha.notification.repository

import com.team1.hangsha.notification.model.NotificationType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class NotificationOutboxRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {
    /**
     * user_id + dedupe_key의 UNIQUE 제약을 이용해 Job 재실행도 안전하게 처리합니다.
     * 반환값 1은 새 행 생성, 0은 이미 같은 알림이 존재함을 뜻합니다.
     */
    fun insertPendingIgnore(
        userId: Long,
        eventId: Long,
        type: NotificationType,
        dedupeKey: String,
        title: String,
        body: String,
        payloadJson: String,
        scheduledAt: LocalDateTime,
    ): Int = jdbc.update(
        """
            INSERT IGNORE INTO notification_outbox (
                user_id, event_id, type, dedupe_key, title, body, payload_json, scheduled_at, status
            ) VALUES (
                :userId, :eventId, :type, :dedupeKey, :title, :body,
                :payloadJson, :scheduledAt, 'PENDING'
            )
        """.trimIndent(),
        mapOf(
            "userId" to userId,
            "eventId" to eventId,
            "type" to type.name,
            "dedupeKey" to dedupeKey,
            "title" to title,
            "body" to body,
            "payloadJson" to payloadJson,
            "scheduledAt" to scheduledAt,
        ),
    )
}
