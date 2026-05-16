package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.shared.StateTransitionValidator

object CatalogImportJobRules {
    private const val MAX_IMPORT_SIZE_BYTES = 10L * 1024L * 1024L

    private val validator = StateTransitionValidator(
        entityName = "catalog import job",
        transitions = mapOf(
            CatalogImportJobStatus.UPLOADED to setOf(CatalogImportJobStatus.MAPPING_REQUIRED, CatalogImportJobStatus.VALIDATING, CatalogImportJobStatus.CANCELED),
            CatalogImportJobStatus.MAPPING_REQUIRED to setOf(CatalogImportJobStatus.VALIDATING, CatalogImportJobStatus.CANCELED),
            CatalogImportJobStatus.VALIDATING to setOf(CatalogImportJobStatus.MATCHED, CatalogImportJobStatus.NEEDS_REVIEW, CatalogImportJobStatus.FAILED),
            CatalogImportJobStatus.MATCHED to setOf(CatalogImportJobStatus.READY_TO_COMMIT, CatalogImportJobStatus.NEEDS_REVIEW, CatalogImportJobStatus.CANCELED),
            CatalogImportJobStatus.NEEDS_REVIEW to setOf(CatalogImportJobStatus.READY_TO_COMMIT, CatalogImportJobStatus.CANCELED),
            CatalogImportJobStatus.READY_TO_COMMIT to setOf(CatalogImportJobStatus.COMMITTED, CatalogImportJobStatus.FAILED, CatalogImportJobStatus.CANCELED),
            CatalogImportJobStatus.COMMITTED to emptySet(),
            CatalogImportJobStatus.FAILED to emptySet(),
            CatalogImportJobStatus.CANCELED to emptySet(),
        ),
    )

    fun validateUpload(filename: String, sizeBytes: Long) {
        val lower = filename.lowercase()
        if (!lower.endsWith(".csv") && !lower.endsWith(".xlsx")) {
            throw DomainRuleViolation("Catalog import file must be CSV or XLSX.")
        }
        if (sizeBytes <= 0) throw DomainRuleViolation("Catalog import file cannot be empty.")
        if (sizeBytes > MAX_IMPORT_SIZE_BYTES) throw DomainRuleViolation("Catalog import file is too large.")
    }

    fun assertCanTransition(from: CatalogImportJobStatus, to: CatalogImportJobStatus) {
        validator.assertCanTransition(from, to)
    }

    fun assertCanCommit(job: CatalogImportJob) {
        if (job.status != CatalogImportJobStatus.READY_TO_COMMIT) {
            throw DomainRuleViolation("Catalog import job must be ready to commit.")
        }
        if (job.errorRows > 0) {
            throw DomainRuleViolation("Catalog import job with errors cannot be committed.")
        }
    }
}
