package com.team1.hangsha.event.model

import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalDateTime

@Table("events")
data class Event(
    @Id
    val id: Long? = null,
    val title: String,
    val imageUrl: String? = null,
    val operationMode: String? = null,

    val statusId: Long? = null,
    val eventTypeId: Long? = null,
    val orgId: Long? = null,

    val applyStart: LocalDateTime? = null,
    val applyEnd: LocalDateTime? = null,
    val eventStart: LocalDateTime? = null,
    val eventEnd: LocalDateTime? = null,
    // 현재 날짜까지는 제대로 들어오는데 크롤링 완성되기 전까지 default time 어케 설정할지?

    val capacity: Int? = null,
    val applyCount: Int = 0,

    val organization: String? = null,
    val location: String? = null,
    val applyLink: String? = null,

    @CreatedDate
    val createdAt: Instant? = null,
    @LastModifiedDate
    val updatedAt: Instant? = null
)