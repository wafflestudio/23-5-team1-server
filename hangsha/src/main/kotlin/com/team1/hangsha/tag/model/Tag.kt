package com.team1.hangsha.tag.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("tags")
data class Tag(
    @Id
    val id: Long? = null,
    val name: String
)