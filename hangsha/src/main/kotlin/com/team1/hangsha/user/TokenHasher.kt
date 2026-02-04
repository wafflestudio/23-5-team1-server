package com.team1.hangsha.user

import org.mindrot.jbcrypt.BCrypt
import org.springframework.stereotype.Component

@Component
class TokenHasher {
    fun hash(raw: String): String = BCrypt.hashpw(raw, BCrypt.gensalt())
    fun matches(raw: String, hashed: String): Boolean = BCrypt.checkpw(raw, hashed)
}