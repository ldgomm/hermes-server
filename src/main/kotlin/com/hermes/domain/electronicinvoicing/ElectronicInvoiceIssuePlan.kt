package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class ElectronicInvoiceIssuePlan(
    val organizationId: String,
    val branchId: String,
    val emissionPointId: String,
    val saleId: String,
    val environment: SriEnvironment,
    val series: SriSeries,
    val sequential: SriSequential,
    val accessKey: SriAccessKey,
    val authorizationNumber: String,
    val plannedAt: Instant,
) {
    init {
        if (organizationId.isBlank()) throw DomainRuleViolation("Electronic invoice issue plan organization id cannot be blank.")
        if (branchId.isBlank()) throw DomainRuleViolation("Electronic invoice issue plan branch id cannot be blank.")
        if (emissionPointId.isBlank()) throw DomainRuleViolation("Electronic invoice issue plan emission point id cannot be blank.")
        if (saleId.isBlank()) throw DomainRuleViolation("Electronic invoice issue plan sale id cannot be blank.")
        if (authorizationNumber != accessKey.value) {
            throw DomainRuleViolation("Offline SRI authorization number must match the access key.")
        }
        if (accessKey.documentType != SriDocumentType.INVOICE) {
            throw DomainRuleViolation("Electronic invoice issue plan requires an invoice access key.")
        }
        if (accessKey.environment != environment) {
            throw DomainRuleViolation("Electronic invoice issue plan environment must match the access key.")
        }
        if (accessKey.series != series) {
            throw DomainRuleViolation("Electronic invoice issue plan series must match the access key.")
        }
        if (accessKey.sequential != sequential) {
            throw DomainRuleViolation("Electronic invoice issue plan sequential must match the access key.")
        }
    }
}
