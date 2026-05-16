package com.hermes.domain.schedule

import com.hermes.domain.shared.DomainRuleViolation
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class BusinessHoursRule(
    val dayOfWeek: DayOfWeek,
    val opensAt: LocalTime?,
    val closesAt: LocalTime?,
    val closed: Boolean = false,
) {
    init {
        if (!closed) {
            if (opensAt == null || closesAt == null) throw DomainRuleViolation("Open business hours require opening and closing times.")
            if (!opensAt.isBefore(closesAt)) throw DomainRuleViolation("Opening time must be before closing time.")
        }
    }
}

data class SpecialHours(
    val date: LocalDate,
    val opensAt: LocalTime?,
    val closesAt: LocalTime?,
    val closed: Boolean = false,
) {
    init {
        if (!closed) {
            if (opensAt == null || closesAt == null) throw DomainRuleViolation("Open special hours require opening and closing times.")
            if (!opensAt.isBefore(closesAt)) throw DomainRuleViolation("Special opening time must be before closing time.")
        }
    }
}

data class TemporaryClosure(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val reason: String,
) {
    init {
        if (endDate.isBefore(startDate)) throw DomainRuleViolation("Temporary closure end date cannot be before start date.")
        if (reason.isBlank()) throw DomainRuleViolation("Temporary closure reason cannot be blank.")
    }
}

data class BusinessHours(
    val weeklyRules: List<BusinessHoursRule>,
    val specialHours: List<SpecialHours> = emptyList(),
    val temporaryClosures: List<TemporaryClosure> = emptyList(),
)
