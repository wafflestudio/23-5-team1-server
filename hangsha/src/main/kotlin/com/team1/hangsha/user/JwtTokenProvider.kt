package com.team1.hangsha.user

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.http.ResponseCookie
import java.util.Date
import java.util.UUID


@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    secretKey: String,

    @Value("\${jwt.access-expiration-ms}")
    private val accessExpirationMs: Long,

    @Value("\${jwt.refresh-expiration-ms}")
    private val refreshExpirationMs: Long,
) {

    private val key = Keys.hmacShaKeyFor(secretKey.toByteArray())

    fun createAccessToken(userId: Long): String {
        return createToken(
            userId = userId,
            expirationMs = accessExpirationMs,
            type = "ACCESS",
        )
    }

    fun createRefreshToken(userId: Long): String {
        return createToken(
            userId = userId,
            expirationMs = refreshExpirationMs,
            type = "REFRESH",
        )
    }

    fun createToken(
        userId: Long,
        expirationMs: Long,
        type: String
    ): String {
        val now = Date()
        val expiry = Date(now.time + expirationMs)

        val jti = UUID.randomUUID().toString()

        return Jwts.builder()
            .setSubject(userId.toString())
            .setId(jti)
            .claim("jti", jti)
            .claim("type", type)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun parseClaims(token: String) =
        Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body

    fun getUserId(token: String): Long =
        parseClaims(token).subject.toLong()

    fun validateAccessToken(token: String): Boolean {
        try {
            val claims = Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body

            // type 체크 (ACCESS 토큰만 인증용 허용)
            val type = claims["type"] as? String
            if (type != "ACCESS") {
                return false
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun validateRefreshToken(token: String): Boolean {
        try {
            val claims = Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body

            val type = claims["type"] as? String
            if (type != "REFRESH") {
                return false
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun getJti(token: String): String =
        parseClaims(token).id

    fun buildRefreshCookie(token: String, maxAgeSeconds: Long): ResponseCookie =
        ResponseCookie.from("refreshToken", token)
            .httpOnly(true)
            .secure(true)         // prod는 true, 로컬 http 테스트면 false 필요
            .sameSite("Lax")
            .path("/api/v1/auth")
            .maxAge(maxAgeSeconds)
            .build()

    fun clearRefreshCookie(): ResponseCookie =
        ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/api/v1/auth")
            .maxAge(0)
            .build()
}