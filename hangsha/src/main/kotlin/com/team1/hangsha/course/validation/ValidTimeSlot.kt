package com.team1.hangsha.course.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [TimeSlotValidator::class])
annotation class ValidTimeSlot(
    val message: String = "startAt must be less than endAt",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)