package com.hermes.infrastructure.mongo.electronicinvoicing

object ElectronicInvoicingMongoCollectionNames {
    const val ELECTRONIC_SEQUENCES: String = "electronic_sequences"
    const val ELECTRONIC_INVOICE_ISSUES: String = "electronic_invoice_issues"
    const val ELECTRONIC_DOCUMENT_ARTIFACTS: String = "electronic_document_artifacts"
}

object ElectronicSequenceMongoCollectionNames {
    const val ELECTRONIC_SEQUENCES: String = ElectronicInvoicingMongoCollectionNames.ELECTRONIC_SEQUENCES
}