package com.team1.hangsha.user.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("user_identities")
data class UserIdentity(
    @Id
    val id: Long? = null,
    val userId: Long,
    val provider: AuthProvider,
    val providerUserId: String,
    val email: String? = null,  // 소셜에서 주는 이메일과 로컬 이메일이 다를 수 있음
    val password: String? = null,
    @CreatedDate
    var createdAt: Instant? = null,
)

// [현재 단계]
// - users.email 과 user_identities.email 은 동일한 값을 가진다.
// - 이메일을 사용자 계정의 대표 식별자로 사용한다.
// - 이미 A 이메일로 가입한 사용자가,
//   동일한 A 이메일로 다른 로그인 방식(로컬/소셜)을 시도할 경우
//   중복 계정 생성을 허용하지 않고 기존 로그인 방식으로 유도한다.
// - 즉, 동일 이메일 기반의 소셜 + 로컬 중복 user 생성은 허용하지 않는다.
// - 로그인 방식(auth provider)에 따라 이메일이 존재할 수도 있고, 존재하지 않을 수도 있다.

// [추후 확장: 계정 연동 기능 도입 시]
// - 서로 다른 이메일을 가진 auth provider 2개 이상을
//   하나의 사용자 계정(users)에 연동할 수 있도록 한다.
// - 이 경우, 각 로그인 방식별 이메일 정보를 보존하기 위해
//   user_identities 테이블에 email 필드가 필요하다.
// - 여러 user_identity가 동일한 users.id(pk)를 참조함으로써,
//   사용자가 로컬/소셜 중 어떤 방식으로 로그인하더라도
//   동일한 사용자로 취급할 수 있다.