package com.team1.hangsha.timetable.repository

import com.team1.hangsha.common.enums.Semester
import com.team1.hangsha.timetable.model.Timetable
import org.springframework.data.repository.CrudRepository

interface TimetableRepository : CrudRepository<Timetable, Long> {
    fun findAllByUserIdOrderByIdDesc(userId: Long): List<Timetable>

    fun findAllByUserIdAndYearOrderByIdDesc(userId: Long, year: Int): List<Timetable>

    fun findAllByUserIdAndSemesterOrderByIdDesc(userId: Long, semester: Semester): List<Timetable>

    fun findAllByUserIdAndYearAndSemesterOrderByIdDesc(userId: Long, year: Int, semester: Semester): List<Timetable>

    fun deleteByIdAndUserId(id: Long, userId: Long): Int

    fun existsByIdAndUserId(id: Long, userId: Long): Boolean
    fun findByIdAndUserId(id: Long, userId: Long): Timetable?
}