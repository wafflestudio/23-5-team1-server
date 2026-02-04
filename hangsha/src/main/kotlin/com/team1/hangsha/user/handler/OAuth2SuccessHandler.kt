package com.team1.hangsha.user.handler

import com.team1.hangsha.user.repository.UserRepository
import com.team1.hangsha.user.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.http.HttpHeaders


@Component
class OAuth2SuccessHandler(
    private val userRepository: UserRepository,
    private val userService: UserService,
    @Value("\${auth.oauth2.frontend-callback-url}")
    private val frontendCallbackUrl: String,
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

        val (refreshCookie, code) = userService.issueRefreshCookieAndOAuthCode(user.id!!)

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString())

        val targetUrl = UriComponentsBuilder
            .fromUriString(frontendCallbackUrl)
            .queryParam("code", code)
            .build()
            .toUriString()

        redirectStrategy.sendRedirect(request, response, targetUrl)
    }
}