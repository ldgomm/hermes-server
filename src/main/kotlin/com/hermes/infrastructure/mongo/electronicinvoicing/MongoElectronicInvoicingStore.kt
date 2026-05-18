package com.hermes.infrastructure.mongo.electronicinvoicing

import com.hermes.application.electronicinvoicing.ElectronicSequenceRepository
import com.mongodb.client.MongoDatabase

class MongoElectronicInvoicingStore(database: MongoDatabase) {
    val sequenceRepository: ElectronicSequenceRepository = MongoElectronicSequenceRepository(database)
}
