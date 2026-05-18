package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SriAccessKeyGeneratorTest {
    @Test
    fun `generates valid 49 digit SRI access key`() {
        val accessKey = SriAccessKeyGenerator.generate(
            SriAccessKeyGenerationCommand(
                issuedDate = LocalDate.of(2026, 5, 18),
                documentType = SriDocumentType.INVOICE,
                ruc = "1790012345001",
                environment = SriEnvironment.TEST,
                series = SriSeries("001", "001"),
                sequential = SriSequential(123),
                numericCode = SriNumericCode("12345678"),
                emissionType = SriEmissionType.NORMAL,
            )
        )

        assertEquals("1805202601179001234500110010010000001231234567812", accessKey.value)
        assertEquals("18052026", accessKey.issuedDateDdmmyyyy)
        assertEquals(SriDocumentType.INVOICE, accessKey.documentType)
        assertEquals("1790012345001", accessKey.ruc)
        assertEquals(SriEnvironment.TEST, accessKey.environment)
        assertEquals(SriSeries("001", "001"), accessKey.series)
        assertEquals(SriSequential(123), accessKey.sequential)
        assertEquals(SriNumericCode("12345678"), accessKey.numericCode)
        assertEquals(SriEmissionType.NORMAL, accessKey.emissionType)
        assertEquals(2, accessKey.checkDigit)
    }

    @Test
    fun `rejects access key with invalid check digit`() {
        assertFailsWith<DomainRuleViolation> {
            SriAccessKey("1805202601179001234500110010010000001231234567813")
        }
    }

    @Test
    fun `rejects invalid issuer RUC`() {
        assertFailsWith<DomainRuleViolation> {
            SriAccessKeyGenerationCommand(
                issuedDate = LocalDate.of(2026, 5, 18),
                documentType = SriDocumentType.INVOICE,
                ruc = "123",
                environment = SriEnvironment.TEST,
                series = SriSeries("001", "001"),
                sequential = SriSequential(123),
                numericCode = SriNumericCode("12345678"),
            )
        }
    }
}
