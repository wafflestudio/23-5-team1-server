package com.team1.hangsha.timetable.repository

import com.team1.hangsha.timetable.model.Enroll
import com.team1.hangsha.timetable.repository.row.EnrollWithCourseRow
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface EnrollRepository : CrudRepository<Enroll, Long> {

    fun existsByTimetableIdAndCourseId(timetableId: Long, courseId: Long): Boolean

    fun findByIdAndTimetableId(id: Long, timetableId: Long): Enroll?

    fun deleteByIdAndTimetableId(id: Long, timetableId: Long): Int

    @Query("""
        SELECT
            e.id AS enroll_id,
            e.timetable_id AS timetable_id,

            c.id AS course_id,
            c.year AS year,
            c.semester AS semester,
            c.course_title AS course_title,
            c.source AS source,
            c.owner_user_id AS owner_user_id,
            c.course_number AS course_number,
            c.lecture_number AS lecture_number,
            c.credit AS credit,
            c.instructor AS instructor
        FROM enrolls e
        JOIN courses c ON c.id = e.course_id
        WHERE e.timetable_id = :timetableId
        ORDER BY e.id DESC
    """)
    fun findAllWithCourseByTimetableId(timetableId: Long): List<EnrollWithCourseRow>

    @Query("""
        SELECT
            e.id AS enroll_id,
            e.timetable_id AS timetable_id,

            c.id AS course_id,
            c.year AS year,
            c.semester AS semester,
            c.course_title AS course_title,
            c.source AS source,
            c.owner_user_id AS owner_user_id,
            c.course_number AS course_number,
            c.lecture_number AS lecture_number,
            c.credit AS credit,
            c.instructor AS instructor
        FROM enrolls e
        JOIN courses c ON c.id = e.course_id
        WHERE e.timetable_id = :timetableId
          AND e.id = :enrollId
        LIMIT 1
    """)
    fun findOneWithCourseByTimetableIdAndEnrollId(
        timetableId: Long,
        enrollId: Long
    ): EnrollWithCourseRow?

    fun deleteAllByTimetableId(timetableId: Long): Int
}