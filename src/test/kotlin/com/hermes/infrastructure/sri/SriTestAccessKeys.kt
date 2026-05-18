package com.hermes.infrastructure.sri

import com.hermes.domain.electronicinvoicing.*
import java.time.LocalDate

internal fun testAccessKey(environment: SriEnvironment = SriEnvironment.TEST): SriAccessKey =
    SriAccessKeyGenerator.generate(
        SriAccessKeyGenerationCommand(
            issuedDate = LocalDate.of(2026, 5, 18),
            documentType = SriDocumentType.INVOICE,
            ruc = "1790012345001",
            environment = environment,
            series = SriSeries("001", "001"),
            sequential = SriSequential(1),
            numericCode = SriNumericCode("12345678"),
            emissionType = SriEmissionType.NORMAL,
        )
    )
