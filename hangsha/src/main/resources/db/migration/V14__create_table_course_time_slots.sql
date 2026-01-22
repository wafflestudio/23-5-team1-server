CREATE TABLE IF NOT EXISTS course_time_slots (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,

    course_id BIGINT NOT NULL,

    day_of_week VARCHAR(3) NOT NULL, -- MON/TUE/WED/THU/FRI/SAT/SUN
    start_at INT NOT NULL,           -- 분 단위 (예: 630 = 10:30)
    end_at INT NOT NULL,

    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    KEY idx_time_slots_course (course_id),
    KEY idx_time_slots_course_day (course_id, day_of_week),

    CONSTRAINT fk_time_slots_course_id
        FOREIGN KEY (course_id) REFERENCES courses(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;