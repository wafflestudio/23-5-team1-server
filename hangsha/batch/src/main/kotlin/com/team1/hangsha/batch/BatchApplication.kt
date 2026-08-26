package com.team1.hangsha.batch

import com.team1.hangsha.com.team1.hangsha.config.JacksonConfig
import com.team1.hangsha.common.upload.OciUploadService
import com.team1.hangsha.config.OciConfig
import com.team1.hangsha.config.TestValueLogger
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Import
import kotlin.system.exitProcess

@SpringBootApplication
@Import(
    JacksonConfig::class,
    TestValueLogger::class,
    OciConfig::class,
    OciUploadService::class,
) // for explicit bean import
class BatchApplication

fun main(args: Array<String>) {
    val batchJob = resolveBatchJob(args)
    val builder = SpringApplicationBuilder(BatchApplication::class.java)
        .web(WebApplicationType.NONE)
        .properties("batch.job=$batchJob")

    if (batchJob in setOf("snu-now-dump", "snu-calendar-dump")) {
        builder.properties(
            mapOf(
                "spring.autoconfigure.exclude" to listOf(
                    DataSourceAutoConfiguration::class.java.name,
                    DataSourceTransactionManagerAutoConfiguration::class.java.name,
                    JdbcTemplateAutoConfiguration::class.java.name,
                    JdbcRepositoriesAutoConfiguration::class.java.name,
                ).joinToString(",")
            )
        )
    }

    val exitCode = try {
        val context = builder.run(*args)
        SpringApplication.exit(context)
    } catch (e: Exception) {
        e.printStackTrace()
        1
    }
    exitProcess(exitCode)
}

private fun resolveBatchJob(args: Array<String>): String =
    System.getenv("BATCH_JOB")?.trim()?.takeIf { it.isNotEmpty() }
        ?: args.firstOrNull { it.startsWith("--batch.job=") }?.substringAfter("=")
        ?: args.firstOrNull { it.startsWith("--job=") }?.substringAfter("=")
        ?: error("BATCH_JOB environment variable is required")
