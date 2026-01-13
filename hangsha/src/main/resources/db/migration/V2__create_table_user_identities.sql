CREATE TABLE IF NOT EXISTS user_identities
(
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,

    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NULL,

    email VARCHAR(255) NULL,
    password VARCHAR(255) NULL,

    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    KEY idx_user_identities_user_id (user_id),

    -- 소셜 로그인 식별자 유니크: (provider, provider_user_id)
    -- LOCAL은 provider_user_id가 NULL이므로 중복 허용(=영향 없음)
    UNIQUE KEY uk_provider_provider_user_id (provider, provider_user_id),

    -- 로컬 로그인 식별자 유니크: (provider, email)
    -- SOCIAL은 email이 NULL일 수 있으니 중복 허용(=영향 거의 없음)
    UNIQUE KEY uk_provider_email (provider, email),

    CONSTRAINT fk_user_identities_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;