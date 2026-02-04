package com.team1.hangsha

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class HangshaApplication

fun main(args: Array<String>) {
    runApplication<HangshaApplication>(*args)
}
