package com.team1.hangsha.user.repository

import com.team1.hangsha.user.model.AuthProvider
import org.springframework.data.repository.CrudRepository
import com.team1.hangsha.user.model.UserIdentity

interface UserIdentityRepository : CrudRepository<UserIdentity, Long> {
    fun findByProviderAndEmail(provider: AuthProvider, email: String): UserIdentity?

    fun findFirstByUserIdAndProvider(userId: Long, provider: AuthProvider): UserIdentity?

    fun findByProviderAndProviderUserId(provider: AuthProvider, providerUserId: String): UserIdentity?

    fun existsByProviderAndEmail(provider: AuthProvider, email: String): Boolean
}