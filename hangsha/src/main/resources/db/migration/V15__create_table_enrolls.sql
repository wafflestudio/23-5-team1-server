CREATE TABLE IF NOT EXISTS enrolls (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,

    timetable_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,

    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    -- 같은 시간표에 같은 강의 중복 등록 방지
    UNIQUE KEY uk_enrolls_timetable_course (timetable_id, course_id),

    KEY idx_enrolls_timetable (timetable_id),
    KEY idx_enrolls_course (course_id),

    CONSTRAINT fk_enrolls_timetable_id
        FOREIGN KEY (timetable_id) REFERENCES timetables(id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_enrolls_course_id
        FOREIGN KEY (course_id) REFERENCES courses(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;