CREATE TABLE IF NOT EXISTS events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    image_url VARCHAR(2048) NULL,
    operation_mode VARCHAR(50) NULL,

    -- category FK (모집현황/프로그램유형/주체기관)
    status_id BIGINT NULL,
    event_type_id BIGINT NULL,
    org_id BIGINT NULL,

    -- 기간: 크롤링 불완전하므로 NULL 허용 (서비스에서 default 채우기)
    apply_start DATETIME NULL,
    apply_end DATETIME NULL,
    event_start DATETIME NULL,
    event_end DATETIME NULL,

    capacity INT NULL,
    apply_count INT NOT NULL DEFAULT 0,

    organization VARCHAR(255) NULL,
    location VARCHAR(255) NULL,
    apply_link VARCHAR(2048) NULL,

    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    KEY idx_events_event_start (event_start),
    KEY idx_events_event_end (event_end),
    KEY idx_events_organization (organization),

    KEY idx_events_status_id (status_id),
    KEY idx_events_event_type_id (event_type_id),
    KEY idx_events_org_id (org_id),

    CONSTRAINT fk_events_status
    FOREIGN KEY (status_id) REFERENCES categories(id),

    CONSTRAINT fk_events_event_type
    FOREIGN KEY (event_type_id) REFERENCES categories(id),

    CONSTRAINT fk_events_org
    FOREIGN KEY (org_id) REFERENCES categories(id)

    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;