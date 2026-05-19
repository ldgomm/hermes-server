package com.hermes.backend.electronicinvoicing

import com.hermes.application.electronicinvoicing.GetElectronicInvoiceUseCase
import com.hermes.application.electronicinvoicing.ListElectronicInvoicesUseCase
import com.hermes.infrastructure.mongo.electronicinvoicing.MongoElectronicInvoiceIssueRepository
import com.mongodb.client.MongoDatabase

object ElectronicInvoicingModuleFactory {
    fun fromMongo(database: MongoDatabase): ElectronicInvoicingModule {
        val issueRepository = MongoElectronicInvoiceIssueRepository(database)
        return ElectronicInvoicingModule(
            getElectronicInvoiceUseCase = GetElectronicInvoiceUseCase(issueRepository),
            listElectronicInvoicesUseCase = ListElectronicInvoicesUseCase(issueRepository),
        )
    }
}
