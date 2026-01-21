package com.team1.hangsha.timetable.repository

import com.team1.hangsha.timetable.model.Course
import org.springframework.data.repository.CrudRepository

interface CourseRepository : CrudRepository<Course, Long> {
    // 지금 서비스에서 findById/save만 쓰니까 별도 메서드 없어도 됨.
    // (추후 크롤링 검색/필터링 들어오면 여기 확장)
}