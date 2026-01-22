package com.team1.hangsha.course.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import com.team1.hangsha.common.enums.DayOfWeek
import java.time.Instant

@Table("course_time_slots")
data class CourseTimeSlot(
    @Id var id: Long? = null,

    var courseId: Long,

    var dayOfWeek: DayOfWeek,

    // 분 단위 (예: 630 = 10:30)
    var startAt: Int,
    var endAt: Int,

    @CreatedDate var createdAt: Instant? = null,
)