package com.team1.hangsha.notification.model

data class NotificationPreference(
    val pushEnabled: Boolean = true,
    val bookmarkEventEnabled: Boolean = true,
    val interestRecommendationEnabled: Boolean = true,
    val weeklyAiRecommendationEnabled: Boolean = true,
)
