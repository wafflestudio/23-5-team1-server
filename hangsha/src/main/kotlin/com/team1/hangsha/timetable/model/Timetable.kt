package com.team1.hangsha.timetable.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Table
import com.team1.hangsha.common.enums.Semester
import java.time.Instant

@Table("timetables")
data class Timetable(
    @Id var id: Long? = null,

    var userId: Long,
    var name: String,

    var year: Int,
    var semester: Semester,

    @CreatedDate var createdAt: Instant? = null,
    @LastModifiedDate var updatedAt: Instant? = null,
)