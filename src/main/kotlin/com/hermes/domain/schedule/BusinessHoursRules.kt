package com.hermes.domain.schedule

import com.hermes.domain.shared.DomainRuleViolation
import java.time.LocalDateTime

object BusinessHoursRules {
    fun validate(hours: BusinessHours) {
        val duplicatedDays = hours.weeklyRules.groupBy { it.dayOfWeek }.filterValues { it.size > 1 }
        if (duplicatedDays.isNotEmpty()) {
            throw DomainRuleViolation("Business hours cannot contain duplicate weekly days.")
        }
    }

    fun isOpenAt(hours: BusinessHours, dateTime: LocalDateTime): Boolean {
        validate(hours)
        val date = dateTime.toLocalDate()
        val time = dateTime.toLocalTime()

        if (hours.temporaryClosures.any { !date.isBefore(it.startDate) && !date.isAfter(it.endDate) }) {
            return false
        }

        val special = hours.specialHours.firstOrNull { it.date == date }
        if (special != null) {
            return !special.closed && !time.isBefore(special.opensAt!!) && time.isBefore(special.closesAt!!)
        }

        val weekly = hours.weeklyRules.firstOrNull { it.dayOfWeek == dateTime.dayOfWeek } ?: return false
        return !weekly.closed && !time.isBefore(weekly.opensAt!!) && time.isBefore(weekly.closesAt!!)
    }
}
