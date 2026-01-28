CREATE TABLE IF NOT EXISTS bookmarks
(
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    UNIQUE KEY uk_bookmarks_user_event (user_id, event_id),

    KEY idx_bookmarks_user_created (user_id, created_at),
    KEY idx_bookmarks_event_id (event_id),

    CONSTRAINT fk_bookmarks_user_id
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_bookmarks_event_id
    FOREIGN KEY (event_id) REFERENCES events(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;