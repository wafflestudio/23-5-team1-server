package com.team1.hangsha.user

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {

    private val pathMatcher = AntPathMatcher()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        println("현재 요청 주소: ${request.requestURI}")
        val token = resolveToken(request)

        if (token != null && jwtTokenProvider.validateAccessToken(token)) {
            val userId = jwtTokenProvider.getUserId(token)

            val authentication = UsernamePasswordAuthenticationToken(userId, null, emptyList())
            SecurityContextHolder.getContext().authentication = authentication
            request.setAttribute("userId", userId)
        }

        /* if (!isPublicPath(request.requestURI)) {
            val userId = request.getAttribute("userId") as? Long
            if (userId == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid access token")
                return
            }
        }
*/
        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization") ?: return null
        return if (bearerToken.startsWith("Bearer ")) bearerToken.substring(7) else null
    }

    private fun isPublicPath(path: String): Boolean =
        pathMatcher.match("/api/v1/auth/**", path) ||
                pathMatcher.match("/swagger-ui/**", path) ||
                pathMatcher.match("/api-docs/**", path) ||
                pathMatcher.match("/api/v1/health", path) ||
                pathMatcher.match("/api-docs/**", path) ||
                pathMatcher.match("/api/v1/events/**", path) ||
                pathMatcher.match("/api/v1/category-groups/**", path) ||
                pathMatcher.match("/api/v1/categories/**", path) ||
                pathMatcher.match("/admin/events/sync", path) // @TODO: 나중에 자동 크롤링 로직 추가되면 "반드시" 삭제해야함!!!!!
}