package com.team1.hangsha.batch.config

import com.team1.hangsha.config.DatabaseConfig
import com.team1.hangsha.notification.repository.NotificationPreferenceRepository
import com.team1.hangsha.notification.repository.NotificationOutboxRepository
import com.team1.hangsha.notification.repository.BookmarkNotificationQueryRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ConditionalOnProperty(
    prefix = "batch",
    name = ["job"],
    havingValue = "bookmark-notification",
)
@Import(
    DatabaseConfig::class,
    BookmarkNotificationQueryRepository::class,
    NotificationOutboxRepository::class,
    NotificationPreferenceRepository::class,
)
class BookmarkNotificationDatabaseConfiguration