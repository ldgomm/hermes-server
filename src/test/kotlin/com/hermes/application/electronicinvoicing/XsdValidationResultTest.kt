package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class XsdValidationResultTest {
    @Test
    fun `valid result targets xsd validated status`() {
        val result = XsdValidationResult.valid("factura_V2.1.0")

        assertTrue(result.valid)
        assertEquals(ElectronicDocumentStatus.XSD_VALIDATED, result.targetStatus)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `invalid result targets xsd invalid status`() {
        val result = XsdValidationResult.invalid(
            schemaVersionCode = "factura_V2.1.0",
            errors = listOf(XsdValidationError(XsdValidationSeverity.ERROR, "Missing detalles", line = 3, column = 12)),
        )

        assertFalse(result.valid)
        assertEquals(ElectronicDocumentStatus.XSD_INVALID, result.targetStatus)
        assertEquals("3:12", result.errors.single().location)
    }

    @Test
    fun `valid result cannot contain errors`() {
        assertFailsWith<DomainRuleViolation> {
            XsdValidationResult(
                schemaVersionCode = "factura_V2.1.0",
                valid = true,
                errors = listOf(XsdValidationError(XsdValidationSeverity.ERROR, "Unexpected")),
            )
        }
    }

    @Test
    fun `invalid result requires errors`() {
        assertFailsWith<DomainRuleViolation> {
            XsdValidationResult.invalid("factura_V2.1.0", errors = emptyList())
        }
    }
}
