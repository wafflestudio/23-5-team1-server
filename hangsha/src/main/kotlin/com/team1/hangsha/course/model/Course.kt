package com.team1.hangsha.timetable.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Table
import com.team1.hangsha.common.enums.Semester
import com.team1.hangsha.common.enums.CourseSource
import java.time.Instant

@Table("courses")
data class Course(
    @Id var id: Long? = null,

    var year: Int,
    var semester: Semester,
    var courseTitle: String,

    var source: CourseSource,

    // CUSTOM이면 userId, CRAWLED면 null 가능
    var ownerUserId: Long? = null,

    // optional
    var courseNumber: String? = null,
    var lectureNumber: String? = null,
    var credit: Int? = null,
    var instructor: String? = null,

    @CreatedDate var createdAt: Instant? = null,
    @LastModifiedDate var updatedAt: Instant? = null,
)