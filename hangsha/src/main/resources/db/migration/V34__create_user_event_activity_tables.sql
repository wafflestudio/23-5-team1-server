-- AI 추천의 대상 판별(최근 2주 접속)용 일 단위 활동 집계
CREATE TABLE user_activity_daily (
    user_id BIGINT NOT NULL,
    activity_date DATE NOT NULL,
    first_active_at TIMESTAMP(6) NOT NULL,
    last_active_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (user_id, activity_date),
    KEY idx_user_activity_daily_recent (activity_date, user_id),
    CONSTRAINT fk_user_activity_daily_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 상세 페이지 노출/체류 시간 원본 로그
CREATE TABLE event_interactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    client_event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    session_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    interaction_type ENUM('DETAIL_OPEN', 'HEARTBEAT', 'DETAIL_CLOSE') NOT NULL,
    visible_duration_ms INT UNSIGNED NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    received_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_event_interactions_client_event (client_event_id),
    KEY idx_event_interactions_user_recent (user_id, occurred_at),
    KEY idx_event_interactions_event_recent (event_id, occurred_at),

    CONSTRAINT fk_event_interactions_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_event_interactions_event
        FOREIGN KEY (event_id) REFERENCES events(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
