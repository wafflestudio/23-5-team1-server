package com.team1.hangsha.notification.model

enum class NotificationType {
    BOOKMARK_APPLY_DEADLINE,
    BOOKMARK_EVENT_START,
    BOOKMARK_CONTEST_DEADLINE,
    INTEREST_NEW_EVENTS,
    WEEKLY_AI_RECOMMENDATION,
}

data class BookmarkNotificationTarget(
    val userId: Long,
    val eventId: Long,
    val eventTitle: String,
)
