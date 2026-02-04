package com.team1.hangsha.common.upload

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "upload")
data class UploadProperties(
    val dir: String,
    val publicBaseUrl: String,
    val maxSizeBytes: Long = 10 * 1024 * 1024,
)