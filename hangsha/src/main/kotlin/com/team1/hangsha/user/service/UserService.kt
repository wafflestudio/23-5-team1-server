package com.team1.hangsha.user.service

import com.team1.hangsha.user.dto.core.UserDto
import com.team1.hangsha.user.model.User
import com.team1.hangsha.user.repository.UserRepository
import com.team1.hangsha.user.JwtTokenProvider
import com.team1.hangsha.common.error.ErrorCode
import com.team1.hangsha.common.error.DomainException
import com.team1.hangsha.user.model.UserIdentity
import com.team1.hangsha.user.repository.UserIdentityRepository
import com.team1.hangsha.user.model.AuthProvider
import com.team1.hangsha.user.model.AuthTokenPair
import org.mindrot.jbcrypt.BCrypt
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userIdentityRepository: UserIdentityRepository,
) {
    fun localRegister(
        email: String,
        password: String,
    ): UserDto {

        if (userIdentityRepository.existsByProviderAndEmail(AuthProvider.LOCAL, email)) {
            throw DomainException(ErrorCode.USER_EMAIL_ALREADY_EXISTS)
        }
        validatePassword(password)

        val encryptedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        val user =
            userRepository.save(
                User(
                    email = email,
                ),
            )

        userIdentityRepository.save(
            UserIdentity(
                userId = user.id!!,
                provider = AuthProvider.LOCAL,
                email = email,
                password = encryptedPassword,
                ),
            )

        return UserDto(user)
    }

    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw DomainException(ErrorCode.PASSWORD_TOO_SHORT)
        }
        if (password.contains("\\s".toRegex())) {
            throw DomainException(ErrorCode.PASSWORD_CONTAINS_WHITESPACE)
        }
        if (
            !password.any { it.isLetter() } ||
            !password.any { it.isDigit() } ||
            !password.any { !it.isLetterOrDigit() }
        ) {
            throw DomainException(ErrorCode.PASSWORD_WEAK)
        }
    }

    fun localLogin(email: String, password: String): AuthTokenPair {
        val identity = userIdentityRepository.findByProviderAndEmail(AuthProvider.LOCAL, email)
            ?: throw DomainException(ErrorCode.AUTH_INVALID_CREDENTIALS)

        val hashed = identity.password
            ?: throw DomainException(ErrorCode.AUTH_INVALID_CREDENTIALS)

        if (!BCrypt.checkpw(password, hashed)) {
            throw DomainException(ErrorCode.AUTH_INVALID_CREDENTIALS)
        }

        val userId = identity.userId

        val accessToken = jwtTokenProvider.createAccessToken(userId = userId)
        val refreshToken = jwtTokenProvider.createRefreshToken(userId = userId)

        return AuthTokenPair(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }

    fun refreshAccessToken(refreshToken: String): String {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw DomainException(ErrorCode.AUTH_INVALID_TOKEN)
        }

        val userId = jwtTokenProvider.getUserId(refreshToken)

        return jwtTokenProvider.createAccessToken(userId = userId)
    }
}