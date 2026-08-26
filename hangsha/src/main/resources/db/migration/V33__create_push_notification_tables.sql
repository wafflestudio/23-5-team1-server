CREATE TABLE push_devices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,

    target_kind ENUM('FID', 'FCM_REGISTRATION_TOKEN') NOT NULL,
    push_target VARCHAR(512) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    platform ENUM('ANDROID', 'IOS') NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    app_version VARCHAR(64) NULL,
    last_seen_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_push_devices_target (target_kind, push_target),
    KEY idx_push_devices_user_active (user_id, is_active, last_seen_at),

    CONSTRAINT fk_push_devices_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification_preferences (
    user_id BIGINT NOT NULL,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    bookmark_event_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    interest_recommendation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    weekly_ai_recommendation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (user_id),
    CONSTRAINT fk_notification_preferences_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    event_id BIGINT NULL,

    type ENUM(
        'BOOKMARK_APPLY_DEADLINE',
        'BOOKMARK_EVENT_START',
        'BOOKMARK_CONTEST_DEADLINE',
        'INTEREST_NEW_EVENTS',
        'WEEKLY_AI_RECOMMENDATION'
    ) NOT NULL,
    dedupe_key VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body VARCHAR(500) NOT NULL,
    payload_json JSON NOT NULL,
    scheduled_at TIMESTAMP(6) NOT NULL,

    status ENUM('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    attempt_count INT UNSIGNED NOT NULL DEFAULT 0,
    locked_at TIMESTAMP(6) NULL,
    locked_by VARCHAR(100) NULL,
    last_error VARCHAR(1000) NULL,
    sent_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_outbox_user_dedupe (user_id, dedupe_key),
    KEY idx_notification_outbox_dispatch (status, scheduled_at, id),
    KEY idx_notification_outbox_event (event_id),

    CONSTRAINT fk_notification_outbox_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_notification_outbox_event
        FOREIGN KEY (event_id) REFERENCES events(id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification_delivery_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    outbox_id BIGINT NOT NULL,
    push_device_id BIGINT NULL,
    attempt_no INT UNSIGNED NOT NULL,
    status ENUM('SENT', 'FAILED') NOT NULL,
    fcm_message_id VARCHAR(255) NULL,
    failure_code VARCHAR(100) NULL,
    failure_message VARCHAR(1000) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_delivery_attempt (outbox_id, push_device_id, attempt_no),
    KEY idx_notification_delivery_device (push_device_id, created_at),

    CONSTRAINT fk_notification_delivery_outbox
        FOREIGN KEY (outbox_id) REFERENCES notification_outbox(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_notification_delivery_device
        FOREIGN KEY (push_device_id) REFERENCES push_devices(id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
