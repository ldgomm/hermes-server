package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

/**
 * Persisted API/Admin run of the SRI homologation harness.
 *
 * The Fase 11 homologation report is intentionally immutable once stored: it is
 * used as production-gate evidence and should be superseded by a new run rather
 * than edited in place.
 */
enum class ElectronicInvoiceHomologationRunStatus(val storageValue: String) {
    RUNNING("running"),
    PASSED("passed"),
    FAILED("failed");

    companion object {
        fun fromStorage(value: String): ElectronicInvoiceHomologationRunStatus {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { it.storageValue == normalized }
                ?: throw DomainRuleViolation("Unknown electronic invoice homologation run status: $value.")
        }
    }
}

data class ElectronicInvoiceHomologationRun(
    val id: String,
    val organizationId: String,
    val status: ElectronicInvoiceHomologationRunStatus,
    val environment: SriEnvironment,
    val requestedByUserId: String,
    val requiredScenarioCodes: Set<ElectronicInvoiceHomologationScenarioCode>,
    val scenarioResults: List<ElectronicInvoiceHomologationScenarioResult>,
    val reportMarkdown: String,
    val productionDecision: ElectronicInvoiceProductionReadinessDecision?,
    val approvedForProduction: Boolean,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long = 1,
    val schemaVersion: Int = SCHEMA_VERSION,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Homologation run id cannot be blank.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Homologation run organization id cannot be blank.")
        if (requestedByUserId.isBlank()) throw DomainRuleViolation("Homologation run requestedByUserId cannot be blank.")
        if (requiredScenarioCodes.isEmpty()) throw DomainRuleViolation("Homologation run requires at least one required scenario code.")
        if (reportMarkdown.isBlank()) throw DomainRuleViolation("Homologation run report cannot be blank.")
        if (version < 1) throw DomainRuleViolation("Homologation run version must be at least 1.")
        if (schemaVersion < 1) throw DomainRuleViolation("Homologation run schema version must be at least 1.")
    }

    val passed: Boolean get() = status == ElectronicInvoiceHomologationRunStatus.PASSED

    companion object {
        const val SCHEMA_VERSION: Int = 1
    }
}

data class ElectronicInvoiceHomologationRunSearchQuery(
    val organizationId: String,
    val statuses: Set<ElectronicInvoiceHomologationRunStatus> = emptySet(),
    val limit: Int = 50,
) {
    init {
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required to search homologation runs.")
        if (limit !in 1..200) throw DomainRuleViolation("Homologation run search limit must be between 1 and 200.")
    }
}
