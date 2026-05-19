package com.hermes.infrastructure.mongo.electronicinvoicing

object ElectronicInvoicingMongoCollectionNames {
    const val ELECTRONIC_SEQUENCES: String = "electronic_sequences"
    const val ELECTRONIC_INVOICE_ISSUES: String = "electronic_invoice_issues"
    const val ELECTRONIC_DOCUMENT_ARTIFACTS: String = "electronic_document_artifacts"
    const val ORGANIZATION_SRI_SETTINGS: String = "organization_sri_settings"
    const val ELECTRONIC_HOMOLOGATION_RUNS: String = "electronic_homologation_runs"
}

object ElectronicSequenceMongoCollectionNames {
    const val ELECTRONIC_SEQUENCES: String = ElectronicInvoicingMongoCollectionNames.ELECTRONIC_SEQUENCES
}
