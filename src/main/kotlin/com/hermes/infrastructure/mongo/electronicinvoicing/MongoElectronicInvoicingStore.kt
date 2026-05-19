package com.hermes.infrastructure.mongo.electronicinvoicing

import com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueAuditLogger
import com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueRepository
import com.hermes.application.electronicinvoicing.ElectronicSequenceRepository
import com.mongodb.client.MongoDatabase

class MongoElectronicInvoicingStore(database: MongoDatabase) {
    val sequenceRepository: ElectronicSequenceRepository = MongoElectronicSequenceRepository(database)
    val issueRepository: ElectronicInvoiceIssueRepository = MongoElectronicInvoiceIssueRepository(database)
    val issueAuditLogger: ElectronicInvoiceIssueAuditLogger = MongoElectronicInvoiceIssueAuditLogger(database)
}