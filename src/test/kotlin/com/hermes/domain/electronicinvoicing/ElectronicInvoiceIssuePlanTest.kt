package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ElectronicInvoiceIssuePlanTest {
    @Test
    fun `creates electronic invoice issue plan when access key matches`() {
        val series = SriSeries("001", "001")
        val sequential = SriSequential(123)
        val accessKey = SriAccessKeyGenerator.generate(
            SriAccessKeyGenerationCommand(
                issuedDate = LocalDate.of(2026, 5, 18),
                documentType = SriDocumentType.INVOICE,
                ruc = "1790012345001",
                environment = SriEnvironment.TEST,
                series = series,
                sequential = sequential,
                numericCode = SriNumericCode("12345678"),
            )
        )

        val plan = ElectronicInvoiceIssuePlan(
            organizationId = "org_1",
            branchId = "br_1",
            emissionPointId = "emi_1",
            saleId = "sale_1",
            environment = SriEnvironment.TEST,
            series = series,
            sequential = sequential,
            accessKey = accessKey,
            authorizationNumber = accessKey.value,
            plannedAt = Instant.parse("2026-05-18T10:00:00Z"),
        )

        assertEquals(accessKey.value, plan.authorizationNumber)
    }

    @Test
    fun `rejects plan when authorization number does not match access key`() {
        val series = SriSeries("001", "001")
        val sequential = SriSequential(123)
        val accessKey = SriAccessKeyGenerator.generate(
            SriAccessKeyGenerationCommand(
                issuedDate = LocalDate.of(2026, 5, 18),
                documentType = SriDocumentType.INVOICE,
                ruc = "1790012345001",
                environment = SriEnvironment.TEST,
                series = series,
                sequential = sequential,
                numericCode = SriNumericCode("12345678"),
            )
        )

        assertFailsWith<DomainRuleViolation> {
            ElectronicInvoiceIssuePlan(
                organizationId = "org_1",
                branchId = "br_1",
                emissionPointId = "emi_1",
                saleId = "sale_1",
                environment = SriEnvironment.TEST,
                series = series,
                sequential = sequential,
                accessKey = accessKey,
                authorizationNumber = "another",
                plannedAt = Instant.parse("2026-05-18T10:00:00Z"),
            )
        }
    }
}
