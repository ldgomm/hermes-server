package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SriSeriesAndSequentialTest {
    @Test
    fun `formats series and sequential for SRI access key`() {
        val series = SriSeries("001", "002")
        val sequential = SriSequential(123)

        assertEquals("001002", series.value)
        assertEquals("001-002", series.displayValue)
        assertEquals("000000123", sequential.formatted)
    }

    @Test
    fun `parses hyphenated and compact series`() {
        assertEquals(SriSeries("001", "002"), SriSeries.parse("001-002"))
        assertEquals(SriSeries("001", "002"), SriSeries.parse("001002"))
    }

    @Test
    fun `rejects invalid series and sequential`() {
        assertFailsWith<DomainRuleViolation> {
            SriSeries("1", "001")
        }
        assertFailsWith<DomainRuleViolation> {
            SriSequential(0)
        }
        assertFailsWith<DomainRuleViolation> {
            SriSequential(1_000_000_000)
        }
    }
}
