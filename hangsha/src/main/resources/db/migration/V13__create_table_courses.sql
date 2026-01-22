CREATE TABLE IF NOT EXISTS courses (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,

    year INT NOT NULL,
    semester VARCHAR(10) NOT NULL, -- SPRING/SUMMER/FALL/WINTER
    course_title VARCHAR(255) NOT NULL,

    source VARCHAR(10) NOT NULL, -- CRAWLED/CUSTOM

    -- CUSTOM 강의 소유자 (CRAWLED면 NULL 가능)
    owner_user_id BIGINT NULL,

    -- optional
    course_number VARCHAR(50) NULL,
    lecture_number VARCHAR(50) NULL,
    credit INT NULL,
    instructor VARCHAR(100) NULL,

    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    KEY idx_courses_year_semester (year, semester),
    KEY idx_courses_owner (owner_user_id),

    CONSTRAINT fk_courses_owner_user_id
        FOREIGN KEY (owner_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;