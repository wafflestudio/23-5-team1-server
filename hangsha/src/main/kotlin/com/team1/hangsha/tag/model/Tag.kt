package com.team1.hangsha.tag.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("tags")
data class Tag(
    @Id
    val id: Long? = null,

    @Column("user_id")
    val userId: Long,

    val name: String
)