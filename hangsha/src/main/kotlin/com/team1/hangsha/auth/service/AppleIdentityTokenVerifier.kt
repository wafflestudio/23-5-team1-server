package com.team1.hangsha.auth.service

import com.team1.hangsha.common.error.DomainException
import com.team1.hangsha.common.error.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Service

data class AppleIdentity(
    val providerUserId: String,
    val email: String?,
)

interface AppleIdentityTokenVerifier {
    fun verify(identityToken: String): AppleIdentity
}

@Service
class NimbusAppleIdentityTokenVerifier(
    @Value("\${auth.apple.allowed-client-ids}") allowedClientIdsValue: String,
) : AppleIdentityTokenVerifier {
    private val allowedClientIds = allowedClientIdsValue
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toSet()

    private val decoder = NimbusJwtDecoder.withJwkSetUri(APPLE_JWK_SET_URI).build().apply {
        val audienceValidator = OAuth2TokenValidator<Jwt> { jwt ->
            if (jwt.audience.any(allowedClientIds::contains)) {
                OAuth2TokenValidatorResult.success()
            } else {
                OAuth2TokenValidatorResult.failure(
                    OAuth2Error("invalid_token", "Apple identity token audience가 올바르지 않습니다.", null),
                )
            }
        }
        setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtValidators.createDefaultWithIssuer(APPLE_ISSUER),
                audienceValidator,
            ),
        )
    }

    init {
        require(allowedClientIds.isNotEmpty()) { "auth.apple.allowed-client-ids must not be empty." }
    }

    override fun verify(identityToken: String): AppleIdentity {
        val jwt = try {
            decoder.decode(identityToken)
        } catch (error: JwtException) {
            throw DomainException(
                ErrorCode.AUTH_INVALID_CREDENTIALS,
                "유효하지 않은 Apple identity token입니다.",
                error,
            )
        }

        val providerUserId = jwt.subject?.trim().orEmpty()
        if (providerUserId.isEmpty()) {
            throw DomainException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Apple 사용자 식별자가 없습니다.")
        }

        val email = jwt.getClaimAsString("email")?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        if (email != null && !isEmailVerified(jwt.claims["email_verified"])) {
            throw DomainException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Apple 이메일이 검증되지 않았습니다.")
        }

        return AppleIdentity(providerUserId = providerUserId, email = email)
    }

    private fun isEmailVerified(claim: Any?): Boolean = when (claim) {
        is Boolean -> claim
        is String -> claim.equals("true", ignoreCase = true)
        else -> false
    }

    private companion object {
        const val APPLE_ISSUER = "https://appleid.apple.com"
        const val APPLE_JWK_SET_URI = "https://appleid.apple.com/auth/keys"
    }
}
