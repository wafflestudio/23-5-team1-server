package com.team1.hangsha.user.service

import com.team1.hangsha.user.model.AuthProvider
import com.team1.hangsha.user.model.User
import com.team1.hangsha.user.model.UserIdentity
import com.team1.hangsha.user.repository.UserIdentityRepository
import com.team1.hangsha.user.repository.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
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
        // 1. 소셜 서비스(카카오/네이버/구글)에서 유저 정보 가져오기
        val oAuth2User = super.loadUser(userRequest)
        val registrationId = userRequest.clientRegistration.registrationId
        val authProvider = AuthProvider.valueOf(registrationId.uppercase())

        // 2. 데이터 추출 (카카오 등의 깊은 JSON 구조 평탄화)
        val attributes = oAuth2User.attributes
        val extractAttributes = extractAttributes(registrationId, attributes)

        val providerId = extractAttributes.providerId
        val email = extractAttributes.email
        val name = extractAttributes.name
        val picture = extractAttributes.picture

        // 3. DB 저장/연동 로직
        // (이미 가입된 유저라도 저장은 안 하더라도 리턴값은 가공해야 하므로,
        //  "return oAuth2User"로 중간에 함수를 끝내버리면 안 됩니다.)

        val existingIdentity = userIdentityRepository.findByProviderAndProviderUserId(authProvider, providerId)

        if (existingIdentity == null) {
            // 신규 연동이 필요한 경우만 실행
            val existingUser = userRepository.findByEmail(email)

            if (existingUser != null) {
                // 이메일은 같은데 소셜 연동만 안 된 경우 -> 연동 정보 추가
                userIdentityRepository.save(
                    UserIdentity(
                        userId = existingUser.id!!,
                        provider = authProvider,
                        providerUserId = providerId,
                        email = email
                    )
                )
            } else {
                // 아예 쌩 신규 유저 -> 유저 생성 + 연동 정보 추가
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
        }

        // 4. 리턴값 생성 (가입 여부와 상관없이 무조건 실행되어야 함!)
        val newAttributes = attributes.toMutableMap()

        // SuccessHandler가 찾기 쉽게 최상위에 넣어줌
        newAttributes["email"] = email
        newAttributes["name"] = name
        newAttributes["picture"] = picture
        newAttributes["providerId"] = providerId

        val userNameAttributeName = userRequest.clientRegistration
            .providerDetails.userInfoEndpoint.userNameAttributeName

        return DefaultOAuth2User(
            oAuth2User.authorities,
            newAttributes,
            userNameAttributeName
        )
    }

    // (extractAttributes 함수와 OAuthAttributes 클래스는 기존과 동일하므로 생략하지 않고 그대로 둡니다)
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
                val id = attributes["id"].toString()
                val account = attributes["kakao_account"] as Map<String, Any>
                val profile = account["profile"] as? Map<String, Any>

                OAuthAttributes(
                    providerId = id,
                    name = profile?.get("nickname") as? String ?: "KakaoUser",
                    email = account["email"] as? String ?: "$id@kakao.anonymous",
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