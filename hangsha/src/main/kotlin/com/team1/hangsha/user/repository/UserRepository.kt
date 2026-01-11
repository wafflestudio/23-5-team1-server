package com.team1.hangsha.user.repository

import org.springframework.data.repository.CrudRepository
import com.team1.hangsha.user.model.User

interface UserRepository : CrudRepository<User, Long> {
    fun findByIdOrNull(id: Long): User?
}