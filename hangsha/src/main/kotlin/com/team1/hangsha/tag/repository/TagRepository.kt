package com.team1.hangsha.tag.repository

import com.team1.hangsha.tag.model.Tag
import org.springframework.data.repository.CrudRepository
import java.util.Optional

interface TagRepository : CrudRepository<Tag, Long> {
    fun findByUserIdAndName(userId: Long, name: String): Optional<Tag>

    fun findAllByUserId(userId: Long): List<Tag>
}