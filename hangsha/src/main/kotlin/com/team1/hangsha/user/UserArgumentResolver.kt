package com.team1.hangsha.user

import com.team1.hangsha.user.model.User
import com.team1.hangsha.user.repository.UserRepository
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.context.request.RequestAttributes

@Component
class UserArgumentResolver(
    private val userRepository: UserRepository,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        if (!parameter.hasParameterAnnotation(LoggedInUser::class.java)) return false
        return User::class.java.isAssignableFrom(parameter.parameterType)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): User? {
        val userId = webRequest.getAttribute("userId", RequestAttributes.SCOPE_REQUEST) as? Long
            ?: return null
        return userRepository.findByIdOrNull(userId)
    }
}