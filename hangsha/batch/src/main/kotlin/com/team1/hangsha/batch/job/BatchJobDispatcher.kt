package com.team1.hangsha.batch.job

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/** batch 애플리케이션에서 유일한 ApplicationRunner입니다. */
@Component
class BatchJobDispatcher(
    jobs: List<BatchJob>,
    @Value("\${batch.job}") private val jobName: String,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    private val jobsByName: Map<String, BatchJob> = jobs
        .flatMap { job -> job.names.map { name -> name to job } }
        .also { pairs -> check(pairs.map { it.first }.toSet().size == pairs.size) { "Duplicate BatchJob name detected" } }
        .toMap()

    override fun run(args: ApplicationArguments) {
        val job = jobsByName[jobName]
            ?: error("Unknown BATCH_JOB=$jobName. available=${jobsByName.keys.sorted()}")

        log.info("Starting batch job: {}", jobName)
        job.run(args)
        log.info("Completed batch job: {}", jobName)
    }
}
