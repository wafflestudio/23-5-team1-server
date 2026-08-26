-- Elice GPU 서버의 추천 계산 결과를 발송 전까지 보관
CREATE TABLE recommendation_runs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    algorithm VARCHAR(50) NOT NULL,
    model_version VARCHAR(100) NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    source_data_cutoff_at TIMESTAMP(6) NOT NULL,
    status ENUM('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMP(6) NULL,
    completed_at TIMESTAMP(6) NULL,
    failure_message VARCHAR(1000) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    KEY idx_recommendation_runs_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_event_recommendations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recommendation_run_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    rank_no INT UNSIGNED NOT NULL,
    score DECIMAL(12, 8) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_event_recommendations_run_user_event (recommendation_run_id, user_id, event_id),
    UNIQUE KEY uk_user_event_recommendations_run_user_rank (recommendation_run_id, user_id, rank_no),
    KEY idx_user_event_recommendations_user_run_rank (user_id, recommendation_run_id, rank_no),

    CONSTRAINT fk_user_event_recommendations_run
        FOREIGN KEY (recommendation_run_id) REFERENCES recommendation_runs(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_user_event_recommendations_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_user_event_recommendations_event
        FOREIGN KEY (event_id) REFERENCES events(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
