CREATE TABLE IF NOT EXISTS user_interest_categories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    priority INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    -- 같은 유저가 같은 카테고리를 중복 관심등록 못하게
    UNIQUE KEY uk_uic_user_category (user_id, category_id),

    -- 우선순위 조회 빠르게 (유저별 정렬)
    KEY idx_uic_user_priority (user_id, priority),

    CONSTRAINT fk_uic_user_id
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_uic_category_id
    FOREIGN KEY (category_id) REFERENCES categories(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;