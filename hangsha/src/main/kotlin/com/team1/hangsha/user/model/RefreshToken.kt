package com.team1.hangsha.user.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("refresh_tokens")
data class RefreshToken(
    @Id val id: Long? = null,

    @Column("user_id")
    val userId: Long,

    val jti: String,

    @Column("token_hash")
    val tokenHash: String,

    @Column("expires_at")
    val expiresAt: Instant,

    @Column("revoked_at")
    val revokedAt: Instant? = null,

    @CreatedDate
    val createdAt: Instant? = null
)