package com.team1.hangsha.user.service

import com.team1.hangsha.user.model.AuthProvider
import com.team1.hangsha.user.model.User
import com.team1.hangsha.user.model.UserIdentity
import com.team1.hangsha.user.repository.UserIdentityRepository
import com.team1.hangsha.user.repository.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
    private val userIdentityRepository: UserIdentityRepository
) : DefaultOAuth2UserService() {

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)

        val registrationId = userRequest.clientRegistration.registrationId

        val attributes = oAuth2User.attributes
        val extractAttributes = extractAttributes(registrationId, attributes)

        val providerId = extractAttributes.providerId
        val email = extractAttributes.email
        val name = extractAttributes.name
        val picture = extractAttributes.picture

        val authProvider = AuthProvider.valueOf(registrationId.uppercase())

        // --- (이하 저장 로직은 기존과 동일) ---

        // 3. 이미 연동된 계정인지 확인
        val existingIdentity = userIdentityRepository.findByProviderAndProviderUserId(authProvider, providerId)
        if (existingIdentity != null) {
            return oAuth2User
        }

        // 4. 이메일로 기존 유저 확인 (계정 연동)
        val existingUser = userRepository.findByEmail(email)

        if (existingUser != null) {
            userIdentityRepository.save(
                UserIdentity(
                    userId = existingUser.id!!,
                    provider = authProvider,
                    providerUserId = providerId,
                    email = email
                )
            )
        } else {
        val savedUser = userRepository.save(
            User(
                email = email,
                username = name,
                profileImageUrl = picture
            )
        )

        userIdentityRepository.save(
            UserIdentity(
                userId = savedUser.id!!,
                provider = authProvider,
                providerUserId = providerId,
                email = email
            )
        )
    }

    return oAuth2User
}

    // 소셜별로 데이터를 꺼내는 도우미 함수
    private fun extractAttributes(registrationId: String, attributes: Map<String, Any>): OAuthAttributes {
        return when (registrationId) {
            "naver" -> {
                val response = attributes["response"] as Map<String, Any>
                OAuthAttributes(
                    providerId = response["id"] as String,
                    name = response["name"] as String,
                    email = response["email"] as String,
                    picture = response["profile_image"] as? String
                )
            }
            "kakao" -> {
                val id = attributes["id"].toString() // 카카오는 id가 숫자(Long)일 수 있음
                val account = attributes["kakao_account"] as Map<String, Any>
                val profile = account["profile"] as? Map<String, Any>

                OAuthAttributes(
                    providerId = id,
                    name = profile?.get("nickname") as? String ?: "KakaoUser",
                    email = account["email"] as? String ?: "$id@kakao.anonymous", // 이메일 동의 안 했을 경우 대비
                    picture = profile?.get("profile_image_url") as? String
                )
            }
            else -> { // Google
                OAuthAttributes(
                    providerId = attributes["sub"] as String,
                    name = attributes["name"] as String,
                    email = attributes["email"] as String,
                    picture = attributes["picture"] as String
                )
            }
        }
    }

    data class OAuthAttributes(
        val providerId: String,
        val name: String,
        val email: String,
        val picture: String?
    )
}