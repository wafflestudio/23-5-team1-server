package com.team1.hangsha.user.repository

import org.springframework.data.repository.CrudRepository
import com.team1.hangsha.user.model.RefreshToken
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query

interface RefreshTokenRepository : CrudRepository<RefreshToken, Long> {
    fun findByJti(jti: String): RefreshToken?

    @Modifying
    @Query("""
        UPDATE refresh_tokens
        SET revoked_at = CURRENT_TIMESTAMP(6)
        WHERE jti = :jti AND revoked_at IS NULL
    """)
    fun revokeIfNotRevoked(jti: String): Int
}