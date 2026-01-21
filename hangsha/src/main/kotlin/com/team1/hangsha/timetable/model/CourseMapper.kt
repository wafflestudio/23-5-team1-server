package com.team1.hangsha.timetable.model

import com.team1.hangsha.timetable.dto.CourseDto
import com.team1.hangsha.timetable.dto.CourseTimeSlotDto

fun Course.toCourseDto(slots: List<CourseTimeSlot>): CourseDto =
    CourseDto(
        id = this.id!!,
        year = this.year,
        semester = this.semester,
        courseTitle = this.courseTitle,
        source = this.source,
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