package com.team1.hangsha.tag.repository

import com.team1.hangsha.tag.model.Tag
import org.springframework.data.repository.CrudRepository
import java.util.Optional

interface TagRepository : CrudRepository<Tag, Long> {
    fun findByName(name: String): Optional<Tag>
    override fun findAll(): List<Tag>
}