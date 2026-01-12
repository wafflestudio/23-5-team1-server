package com.team1.hangsha

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@Tag(name = "System")
@RestController
class HealthController {

    @Operation(summary = "Health check", description = "서비스 상태 확인(인증 불필요)")
    @CrossOrigin(origins = ["https://d3iy34kj6pk4ke.cloudfront.net"])
    @GetMapping("/api/v1/health")
    fun health() = mapOf(
        "status" to "ok",
        "serverTime" to Instant.now().toString()
    )
}
