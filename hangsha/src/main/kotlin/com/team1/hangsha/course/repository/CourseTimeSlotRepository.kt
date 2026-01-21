package com.team1.hangsha.timetable.repository

import com.team1.hangsha.timetable.model.CourseTimeSlot
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface CourseTimeSlotRepository : CrudRepository<CourseTimeSlot, Long> {

    // 단건 courseId 슬롯 조회 (정렬 포함)
    fun findAllByCourseIdOrderByDayOfWeekAscStartAtAsc(
        courseId: Long
    ): List<CourseTimeSlot>

    // ✅ listEnrolls에서 N+1 없애려고 IN 조회 제공
    @Query("""
        SELECT *
        FROM course_time_slots
        WHERE course_id IN (:courseIds)
        ORDER BY course_id, day_of_week, start_at
    """)
    fun findAllByCourseIdsOrderByCourseIdAscDayOfWeekAscStartAtAsc(
        courseIds: Collection<Long>
    ): List<CourseTimeSlot>

    // replace-all 업데이트용
    fun deleteAllByCourseId(courseId: Long): Int

    fun findAllByCourseId(courseId: Long): List<CourseTimeSlot>
}