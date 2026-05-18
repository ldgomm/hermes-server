package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.SriDocumentType
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriNumericCode
import com.hermes.domain.electronicinvoicing.SriSeries
import java.time.Instant
import java.time.LocalDate

data class EnsureElectronicSequenceCommand(
    val id: String,
    val organizationId: String,
    val environment: SriEnvironment,
    val documentType: SriDocumentType,
    val series: SriSeries,
    val startsAfter: Int = 0,
    val now: Instant,
)

data class NextElectronicSequentialCommand(
    val organizationId: String,
    val environment: SriEnvironment,
    val documentType: SriDocumentType,
    val series: SriSeries,
    val documentId: String? = null,
    val issuedAt: Instant,
)

data class ReserveSriAccessKeyCommand(
    val organizationId: String,
    val environment: SriEnvironment,
    val documentType: SriDocumentType = SriDocumentType.INVOICE,
    val ruc: String,
    val series: SriSeries,
    val issuedDate: LocalDate,
    val numericCode: SriNumericCode,
    val documentId: String? = null,
    val issuedAt: Instant,
)
