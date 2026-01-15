package com.team1.hangsha.common.error

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val httpStatus: HttpStatus,
    val message: String
) {
    // Common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다"),

    // Auth
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다"),
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    USER_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다"),

    // Password policy
    PASSWORD_TOO_SHORT(
        HttpStatus.BAD_REQUEST,
        "비밀번호는 8자 이상이어야 합니다"
    ),
    PASSWORD_WEAK(
        HttpStatus.BAD_REQUEST,
        "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다"
    ),
    PASSWORD_CONTAINS_WHITESPACE(
        HttpStatus.BAD_REQUEST,
        "비밀번호에 공백을 사용할 수 없습니다"
    ),

    // Events
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다"),

    // Category
    CATEGORY_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리 그룹을 찾을 수 없습니다"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다"),
    CATEGORY_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "카테고리 생성에 실패했습니다"),
}