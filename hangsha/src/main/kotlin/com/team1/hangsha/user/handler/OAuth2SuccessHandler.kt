package com.team1.hangsha.user.handler

import com.team1.hangsha.user.JwtTokenProvider
import com.team1.hangsha.user.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class OAuth2SuccessHandler(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository
) : SimpleUrlAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oAuth2User = authentication.principal as OAuth2User
        val email = oAuth2User.attributes["email"] as String

        val user = userRepository.findByEmail(email)
            ?: throw RuntimeException("User not found after OAuth2 login")

        val accessToken = jwtTokenProvider.createAccessToken(user.id!!)

        // 프론트엔드 주소로 변경

        val targetUrl = UriComponentsBuilder.fromUriString("https://d3iy34kj6pk4ke.cloudfront.net/")
            .fragment("accessToken=$accessToken") // #accessToken=... 형태로 붙음
            .build().toUriString()
        redirectStrategy.sendRedirect(request, response, targetUrl)
    }
}