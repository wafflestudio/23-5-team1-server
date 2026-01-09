package com.team1.hangsha.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI {
        val jwtSchemeName = "bearerAuth"

        val securityRequirement = SecurityRequirement().addList(jwtSchemeName)

        val components = Components()
            .addSecuritySchemes(
                jwtSchemeName,
                SecurityScheme()
                    .name(jwtSchemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            )

        return OpenAPI()
            .info(apiInfo())
            .addSecurityItem(securityRequirement) // 모든 API에 전역적으로 보안 적용 (선택 사항, 개별 적용 시 제거 가능)
            .components(components)
            .addServersItem(Server().url("/")) // Swagger UI에서 기본 경로 설정
    }

    private fun apiInfo() = Info()
        .title("Campus Event Calendar API")
        .description("학내 행사 캘린더 서비스 API 명세서")
        .version("1.0.0")
}