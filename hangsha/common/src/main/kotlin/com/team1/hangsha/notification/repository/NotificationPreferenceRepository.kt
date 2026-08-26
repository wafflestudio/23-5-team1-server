package com.team1.hangsha.notification.repository

import com.team1.hangsha.notification.model.NotificationPreference
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class NotificationPreferenceRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {
    fun findByUserId(userId: Long): NotificationPreference? = jdbc.query(
        """
            SELECT push_enabled, bookmark_event_enabled,
                   interest_recommendation_enabled, weekly_ai_recommendation_enabled
            FROM notification_preferences
            WHERE user_id = :userId
        """.trimIndent(),
        mapOf("userId" to userId),
    ) { rs, _ ->
        NotificationPreference(
            pushEnabled = rs.getBoolean("push_enabled"),
            bookmarkEventEnabled = rs.getBoolean("bookmark_event_enabled"),
            interestRecommendationEnabled = rs.getBoolean("interest_recommendation_enabled"),
            weeklyAiRecommendationEnabled = rs.getBoolean("weekly_ai_recommendation_enabled"),
        )
    }.firstOrNull()

    fun upsert(userId: Long, preference: NotificationPreference) {
        jdbc.update(
            """
                INSERT INTO notification_preferences (
                    user_id,
                    push_enabled,
                    bookmark_event_enabled,
                    interest_recommendation_enabled,
                    weekly_ai_recommendation_enabled
                ) VALUES (
                    :userId,
                    :pushEnabled,
                    :bookmarkEventEnabled,
                    :interestRecommendationEnabled,
                    :weeklyAiRecommendationEnabled
                )
                ON DUPLICATE KEY UPDATE
                    push_enabled = VALUES(push_enabled),
                    bookmark_event_enabled = VALUES(bookmark_event_enabled),
                    interest_recommendation_enabled = VALUES(interest_recommendation_enabled),
                    weekly_ai_recommendation_enabled = VALUES(weekly_ai_recommendation_enabled)
            """.trimIndent(),
            mapOf(
                "userId" to userId,
                "pushEnabled" to preference.pushEnabled,
                "bookmarkEventEnabled" to preference.bookmarkEventEnabled,
                "interestRecommendationEnabled" to preference.interestRecommendationEnabled,
                "weeklyAiRecommendationEnabled" to preference.weeklyAiRecommendationEnabled,
            ),
        )
    }
}
