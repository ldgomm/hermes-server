package com.hermes.domain.schedule

import com.hermes.domain.shared.DomainRuleViolation
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BusinessHoursRulesTest {
    @Test
    fun `returns open inside weekly hours`() {
        val hours = BusinessHours(
            weeklyRules = listOf(BusinessHoursRule(DayOfWeek.SATURDAY, LocalTime.of(11, 0), LocalTime.of(17, 0))),
        )

        assertTrue(BusinessHoursRules.isOpenAt(hours, LocalDateTime.of(2026, 5, 16, 12, 0)))
        assertFalse(BusinessHoursRules.isOpenAt(hours, LocalDateTime.of(2026, 5, 16, 18, 0)))
    }

    @Test
    fun `special hours override weekly hours`() {
        val hours = BusinessHours(
            weeklyRules = listOf(BusinessHoursRule(DayOfWeek.SATURDAY, LocalTime.of(11, 0), LocalTime.of(17, 0))),
            specialHours = listOf(SpecialHours(LocalDate.of(2026, 5, 16), null, null, closed = true)),
        )

        assertFalse(BusinessHoursRules.isOpenAt(hours, LocalDateTime.of(2026, 5, 16, 12, 0)))
    }

    @Test
    fun `temporary closure blocks opening`() {
        val hours = BusinessHours(
            weeklyRules = listOf(BusinessHoursRule(DayOfWeek.SATURDAY, LocalTime.of(11, 0), LocalTime.of(17, 0))),
            temporaryClosures = listOf(TemporaryClosure(LocalDate.of(2026, 5, 16), LocalDate.of(2026, 5, 17), "Mantenimiento")),
        )

        assertFalse(BusinessHoursRules.isOpenAt(hours, LocalDateTime.of(2026, 5, 16, 12, 0)))
    }

    @Test
    fun `rejects duplicate weekly day rules`() {
        val hours = BusinessHours(
            weeklyRules = listOf(
                BusinessHoursRule(DayOfWeek.SATURDAY, LocalTime.of(11, 0), LocalTime.of(17, 0)),
                BusinessHoursRule(DayOfWeek.SATURDAY, LocalTime.of(18, 0), LocalTime.of(20, 0)),
            ),
        )

        assertFailsWith<DomainRuleViolation> { BusinessHoursRules.validate(hours) }
    }
}
