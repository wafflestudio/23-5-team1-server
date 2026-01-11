package com.team1.hangsha.common.error

import com.team1.hangsha.common.dto.ErrorResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalControllerExceptionHandler {

    @ExceptionHandler(DomainException::class)
    fun handleDomainException(e: DomainException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(e.errorCode.httpStatus)
            .body(
                ErrorResponse(
                    code = e.errorCode.name,    // enum 상수 이름
                    message = e.message
                )
            )
    }
}