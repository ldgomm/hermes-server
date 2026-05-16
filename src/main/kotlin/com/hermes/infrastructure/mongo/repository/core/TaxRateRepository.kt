package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Sorts
import org.bson.Document

class TaxRateRepository(database: MongoDatabase) : DocumentMongoRepository(database, MongoCollectionNames.TAX_RATES) {
    fun findBySriCodes(countryCode: String, sriTaxCode: String, sriRateCode: String): Document? = findOne(
        and(eq("countryCode", countryCode.uppercase()), eq("sriTaxCode", sriTaxCode), eq("sriRateCode", sriRateCode)),
    )

    fun findActiveByTaxName(countryCode: String, taxName: String): List<Document> = findMany(
        filter = and(eq("countryCode", countryCode.uppercase()), eq("taxName", taxName), eq("status", "active")),
        sort = Sorts.descending("effectiveFrom"),
    )
}
