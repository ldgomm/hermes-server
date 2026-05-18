package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.*

class Modulo11CalculatorTest {
    @Test
    fun `calculates check digit using access key body weights`() {
        val body = "180520260117900123450011001001000000123123456781"

        assertEquals(2, Modulo11Calculator.calculateCheckDigit(body))
        assertTrue(Modulo11Calculator.verify(body + "2"))
        assertFalse(Modulo11Calculator.verify(body + "3"))
    }

    @Test
    fun `rejects non numeric input`() {
        assertFailsWith<DomainRuleViolation> {
            Modulo11Calculator.calculateCheckDigit("123A")
        }
    }
}
