package com.team1.hangsha.timetable.repository.row

import com.team1.hangsha.common.enums.CourseSource
import com.team1.hangsha.common.enums.Semester

data class EnrollWithCourseRow(
    // enroll
    val enrollId: Long,
    val timetableId: Long,

    // course
    val courseId: Long,
    val year: Int,
    val semester: Semester,
    val courseTitle: String,
    val source: CourseSource,
    val ownerUserId: Long?,

    val courseNumber: String?,
    val lectureNumber: String?,
    val credit: Int?,
    val instructor: String?,
)