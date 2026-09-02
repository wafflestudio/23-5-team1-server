package com.team1.hangsha.notification.dto

import com.team1.hangsha.notification.model.NotificationPreference

data class ReplaceNotificationPreferenceRequest(
    val pushEnabled: Boolean,
    val bookmarkEventEnabled: Boolean,
    val interestRecommendationEnabled: Boolean,
    val weeklyAiRecommendationEnabled: Boolean,
)

data class NotificationPreferenceResponse(
    val pushEnabled: Boolean,
    val bookmarkEventEnabled: Boolean,
    val interestRecommendationEnabled: Boolean,
    val weeklyAiRecommendationEnabled: Boolean,
) {
    constructor(preference: NotificationPreference) : this(
        pushEnabled = preference.pushEnabled,
        bookmarkEventEnabled = preference.bookmarkEventEnabled,
        interestRecommendationEnabled = preference.interestRecommendationEnabled,
        weeklyAiRecommendationEnabled = preference.weeklyAiRecommendationEnabled,
    )
}
