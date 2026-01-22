CREATE TABLE IF NOT EXISTS timetables (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,

    year INT NOT NULL,
    semester VARCHAR(10) NOT NULL,  -- SPRING/SUMMER/FALL/WINTER

    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    KEY idx_timetables_user (user_id),
    KEY idx_timetables_user_year_semester (user_id, year, semester),

    CONSTRAINT fk_timetables_user_id
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;