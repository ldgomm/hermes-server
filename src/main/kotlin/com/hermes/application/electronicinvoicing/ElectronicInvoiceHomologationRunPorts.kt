package com.hermes.application.electronicinvoicing

import java.util.*

fun interface ElectronicInvoiceHomologationRunIdGenerator {
    fun newId(prefix: String): String
}

const val ELECTRONIC_INVOICE_HOMOLOGATION_RUN_ID_PREFIX = "homologation_run"

class UuidElectronicInvoiceHomologationRunIdGenerator : ElectronicInvoiceHomologationRunIdGenerator {
    override fun newId(prefix: String): String =
        prefix.trim().lowercase() + "_" + UUID.randomUUID().toString().replace("-", "")
}

interface ElectronicInvoiceHomologationRunRepository {
    fun create(run: ElectronicInvoiceHomologationRun)
    fun findById(organizationId: String, runId: String): ElectronicInvoiceHomologationRun?
    fun search(query: ElectronicInvoiceHomologationRunSearchQuery): List<ElectronicInvoiceHomologationRun>
    fun findLatestApprovedForProduction(organizationId: String): ElectronicInvoiceHomologationRun?
}

data class SriEndpointGateConfig(
    val testReceptionWsdlUrl: String,
    val testAuthorizationWsdlUrl: String,
    val productionReceptionWsdlUrl: String,
    val productionAuthorizationWsdlUrl: String,
    val productionGloballyEnabled: Boolean = false,
) {
    val testEndpoints: ElectronicInvoiceHomologationEndpointConfig
        get() = ElectronicInvoiceHomologationEndpointConfig(
            receptionWsdlUrl = testReceptionWsdlUrl,
            authorizationWsdlUrl = testAuthorizationWsdlUrl,
        )

    val productionEndpoints: ElectronicInvoiceHomologationEndpointConfig
        get() = ElectronicInvoiceHomologationEndpointConfig(
            receptionWsdlUrl = productionReceptionWsdlUrl,
            authorizationWsdlUrl = productionAuthorizationWsdlUrl,
        )

    val productionEndpointsConfigured: Boolean
        get() = productionEndpoints.looksLikeSriProduction && !productionEndpoints.looksLikeSriTest
}
