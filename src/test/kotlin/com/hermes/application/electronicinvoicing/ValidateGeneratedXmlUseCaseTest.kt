package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.electronicinvoicing.SriInvoiceSchemaVersion
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ValidateGeneratedXmlUseCaseTest {
    @Test
    fun `valid xml moves document to xsd validated`() {
        val useCase = ValidateGeneratedXmlUseCase(FakeSriXsdValidator(XsdValidationResult.valid("factura_V2.1.0")))

        val result = useCase.execute(
            ValidateGeneratedXmlCommand(
                documentId = "doc_1",
                generatedXml = GeneratedXml.of(SriInvoiceSchemaVersion.V2_1_0, validXml()),
            )
        )

        assertTrue(result.valid)
        assertEquals("doc_1", result.documentId)
        assertEquals(ElectronicDocumentStatus.XSD_VALIDATED, result.targetStatus)
        assertEquals(64, result.xmlSha256.length)
    }

    @Test
    fun `invalid xml moves document to xsd invalid`() {
        val useCase = ValidateGeneratedXmlUseCase(
            FakeSriXsdValidator(
                XsdValidationResult.invalid(
                    "factura_V2.1.0",
                    listOf(XsdValidationError(XsdValidationSeverity.ERROR, "Missing required tag")),
                )
            )
        )

        val result = useCase.execute(
            ValidateGeneratedXmlCommand(generatedXml = GeneratedXml.of(SriInvoiceSchemaVersion.V2_1_0, validXml()))
        )

        assertEquals(ElectronicDocumentStatus.XSD_INVALID, result.targetStatus)
        assertEquals("Missing required tag", result.errors.single().message)
    }

    @Test
    fun `rejects validation from an invalid current status`() {
        val useCase = ValidateGeneratedXmlUseCase(FakeSriXsdValidator(XsdValidationResult.valid("factura_V2.1.0")))

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                ValidateGeneratedXmlCommand(
                    generatedXml = GeneratedXml.of(SriInvoiceSchemaVersion.V2_1_0, validXml()),
                    currentStatus = ElectronicDocumentStatus.DRAFT,
                )
            )
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
}

private class FakeSriXsdValidator(
    private val result: XsdValidationResult,
) : SriXsdValidator {
    override fun validate(xml: ByteArray, schemaVersionCode: String): XsdValidationResult = result
}
