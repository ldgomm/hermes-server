package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.electronicinvoicing.SriEnvironment
import java.time.Instant

data class GetElectronicInvoiceCommand(
    val organizationId: String,
    val documentId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class ListElectronicInvoicesCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val saleId: String? = null,
    val statuses: Set<ElectronicDocumentStatus> = emptySet(),
    val environment: SriEnvironment? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = ElectronicInvoiceIssueSearchQuery.DEFAULT_LIMIT,
)
