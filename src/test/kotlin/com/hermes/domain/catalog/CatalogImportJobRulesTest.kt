package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CatalogImportJobRulesTest {
    @Test
    fun `allows csv or xlsx uploads`() {
        CatalogImportJobRules.validateUpload("products.csv", 1024)
        CatalogImportJobRules.validateUpload("products.xlsx", 1024)
    }

    @Test
    fun `rejects unsupported upload extension`() {
        assertFailsWith<DomainRuleViolation> {
            CatalogImportJobRules.validateUpload("products.txt", 1024)
        }
    }

    @Test
    fun `allows normal import job flow`() {
        CatalogImportJobRules.assertCanTransition(CatalogImportJobStatus.UPLOADED, CatalogImportJobStatus.MAPPING_REQUIRED)
        CatalogImportJobRules.assertCanTransition(CatalogImportJobStatus.MAPPING_REQUIRED, CatalogImportJobStatus.VALIDATING)
        CatalogImportJobRules.assertCanTransition(CatalogImportJobStatus.VALIDATING, CatalogImportJobStatus.MATCHED)
        CatalogImportJobRules.assertCanTransition(CatalogImportJobStatus.MATCHED, CatalogImportJobStatus.READY_TO_COMMIT)
        CatalogImportJobRules.assertCanTransition(CatalogImportJobStatus.READY_TO_COMMIT, CatalogImportJobStatus.COMMITTED)
    }

    @Test
    fun `rejects committing job with errors`() {
        assertFailsWith<DomainRuleViolation> {
            CatalogImportJobRules.assertCanCommit(
                CatalogImportJob(
                    id = "imp_1",
                    organizationId = "org_1",
                    filename = "products.csv",
                    sizeBytes = 1024,
                    status = CatalogImportJobStatus.READY_TO_COMMIT,
                    totalRows = 10,
                    errorRows = 1,
                ),
            )
        }
    }
}
