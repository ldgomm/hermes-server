package com.hermes.infrastructure.xml

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JaxpSriXsdValidatorTest {
    private val validator = JaxpSriXsdValidator(
        ClasspathXsdSchemaSource(
            resourcesBySchemaVersionCode = mapOf("factura_V2.1.0" to "sri/xsd/factura_V2.1.0.test.xsd"),
        )
    )

    @Test
    fun `validates xml that satisfies schema`() {
        val result = validator.validate(validXml().toByteArray(Charsets.UTF_8), "factura_V2.1.0")

        assertTrue(result.valid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `returns validation errors when xml violates schema`() {
        val result = validator.validate(missingDetailsXml().toByteArray(Charsets.UTF_8), "factura_V2.1.0")

        assertFalse(result.valid)
        assertTrue(result.errors.isNotEmpty())
        assertEquals("factura_V2.1.0", result.schemaVersionCode)
    }

    @Test
    fun `returns validation errors when xml is not well formed`() {
        val result = validator.validate("<factura>".toByteArray(Charsets.UTF_8), "factura_V2.1.0")

        assertFalse(result.valid)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `throws when schema resource is not configured`() {
        val missing = JaxpSriXsdValidator(ClasspathXsdSchemaSource(resourcesBySchemaVersionCode = emptyMap()))

        assertFailsWith<DomainRuleViolation> {
            missing.validate(validXml().toByteArray(Charsets.UTF_8), "factura_V2.1.0")
        }
    }

    private fun validXml(): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <factura id="comprobante" version="2.1.0">
          <infoTributaria/>
          <infoFactura/>
          <detalles/>
        </factura>
    """.trimIndent()

    private fun missingDetailsXml(): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <factura id="comprobante" version="2.1.0">
          <infoTributaria/>
          <infoFactura/>
        </factura>
    """.trimIndent()
}
