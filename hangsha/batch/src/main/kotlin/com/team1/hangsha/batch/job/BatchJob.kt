package com.team1.hangsha.batch.job

import org.springframework.boot.ApplicationArguments

/** Dispatcher만 실행하며, 각 작업은 이 인터페이스를 구현한 Service로 등록합니다. */
interface BatchJob {
    val names: Set<String>

    fun run(args: ApplicationArguments)
}
