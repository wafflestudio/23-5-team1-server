package com.team1.hangsha.course.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import com.team1.hangsha.course.dto.core.CourseTimeSlotDto

class TimeSlotValidator : ConstraintValidator<ValidTimeSlot, CourseTimeSlotDto> {
    override fun isValid(value: CourseTimeSlotDto?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true
        return value.startAt < value.endAt
    }
}