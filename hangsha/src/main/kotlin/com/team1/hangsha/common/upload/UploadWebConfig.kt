package com.team1.hangsha.common.upload

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

@Configuration
class UploadWebConfig(
    private val uploadProperties: UploadProperties,
) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val uploadPath = Paths.get(uploadProperties.dir).toAbsolutePath().normalize().toString()
        registry.addResourceHandler("/static/**")
            .addResourceLocations("file:$uploadPath/")
            .setCachePeriod(3600)
    }
}