CREATE TABLE IF NOT EXISTS user_identities
(
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,

    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,

    email VARCHAR(255) NULL,
    password VARCHAR(255) NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    KEY idx_user_identities_user_id (user_id),
    UNIQUE KEY uk_provider_provider_user_id (provider, provider_user_id),

    CONSTRAINT fk_user_identities_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;