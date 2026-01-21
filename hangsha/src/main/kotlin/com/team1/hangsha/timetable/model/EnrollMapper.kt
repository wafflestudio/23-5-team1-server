package com.team1.hangsha.timetable.repository.row

import com.team1.hangsha.timetable.dto.CourseDto
import com.team1.hangsha.timetable.dto.CourseTimeSlotDto
import com.team1.hangsha.timetable.model.CourseTimeSlot

fun EnrollWithCourseRow.toCourseDto(slots: List<CourseTimeSlot>): CourseDto =
    CourseDto(
        id = this.courseId,
        year = this.year,
        semester = this.semester,
        courseTitle = this.courseTitle,
        source = this.source, // String이면 DTO도 String으로 맞추거나 enum 변환
        timeSlots = slots.map {
            CourseTimeSlotDto(
                dayOfWeek = it.dayOfWeek,
                startAt = it.startAt,
                endAt = it.endAt
            )
        },
        courseNumber = this.courseNumber,
        lectureNumber = this.lectureNumber,
        credit = this.credit,
        instructor = this.instructor
    )