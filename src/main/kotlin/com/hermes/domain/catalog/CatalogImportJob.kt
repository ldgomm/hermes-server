package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation

data class CatalogImportJob(
    val id: String,
    val organizationId: String,
    val filename: String,
    val sizeBytes: Long,
    val status: CatalogImportJobStatus,
    val totalRows: Int = 0,
    val errorRows: Int = 0,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Catalog import job id cannot be blank.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Catalog import job organization id cannot be blank.")
        if (filename.isBlank()) throw DomainRuleViolation("Catalog import job filename cannot be blank.")
        if (sizeBytes <= 0) throw DomainRuleViolation("Catalog import file size must be greater than zero.")
        if (totalRows < 0) throw DomainRuleViolation("Catalog import total rows cannot be negative.")
        if (errorRows < 0) throw DomainRuleViolation("Catalog import error rows cannot be negative.")
    }
}
