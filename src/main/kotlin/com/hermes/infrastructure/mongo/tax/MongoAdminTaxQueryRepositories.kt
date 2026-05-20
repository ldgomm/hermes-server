package com.hermes.infrastructure.mongo.tax

import com.hermes.application.admin.tax.AdminTaxProfileQueryRepository
import com.hermes.application.admin.tax.AdminTaxProfileSearchQuery
import com.hermes.application.admin.tax.AdminTaxRateQueryRepository
import com.hermes.application.admin.tax.AdminTaxRateSearchQuery
import com.hermes.domain.tax.TaxProfile
import com.hermes.domain.tax.TaxRate
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.bson.conversions.Bson

class MongoAdminTaxRateQueryRepository(
    database: MongoDatabase,
) : AdminTaxRateQueryRepository {
    private val collection: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.TAX_RATES)

    override fun search(query: AdminTaxRateSearchQuery): List<TaxRate> {
        val filters = mutableListOf<Bson>()
        query.kind?.let { filters += eq("kind", it.name) }
        if (query.statuses.isNotEmpty()) filters += `in`("status", query.statuses.map { it.name })
        query.query?.trim()?.takeIf { it.isNotBlank() }?.let { text ->
            filters += or(
                regex("code", text.lowercase(), "i"),
                regex("name", text, "i"),
                regex("legalBasis", text, "i"),
                regex("sriTaxCode", text, "i"),
                regex("sriRateCode", text, "i"),
            )
        }
        query.effectiveAt?.let { instant ->
            val date = MongoInstantMapper.toDate(instant)
            filters += lte("effectiveFrom", date)
            filters += or(eq("effectiveTo", null), gt("effectiveTo", date))
        }

        val finalFilter = if (filters.isEmpty()) Document() else and(filters)
        return collection.find(finalFilter)
            .sort(Sorts.ascending("kind", "code"))
            .limit(query.limit.coerceIn(1, 250))
            .into(mutableListOf())
            .map(MongoTaxMappers::taxRateFromDocument)
    }
}

class MongoAdminTaxProfileQueryRepository(
    database: MongoDatabase,
) : AdminTaxProfileQueryRepository {
    private val collection: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.TAX_PROFILES)

    override fun search(query: AdminTaxProfileSearchQuery): List<TaxProfile> {
        val filters = mutableListOf<Bson>()
        query.treatment?.let { filters += eq("treatment", it.name) }
        if (query.statuses.isNotEmpty()) filters += `in`("status", query.statuses.map { it.name })
        query.query?.trim()?.takeIf { it.isNotBlank() }?.let { text ->
            filters += or(
                regex("code", text.lowercase(), "i"),
                regex("name", text, "i"),
                regex("legalBasis", text, "i"),
                regex("sriTaxCode", text, "i"),
                regex("sriRateCode", text, "i"),
            )
        }
        query.effectiveAt?.let { instant ->
            val date = MongoInstantMapper.toDate(instant)
            filters += lte("effectiveFrom", date)
            filters += or(eq("effectiveTo", null), gt("effectiveTo", date))
        }

        val finalFilter = if (filters.isEmpty()) Document() else and(filters)
        return collection.find(finalFilter)
            .sort(Sorts.ascending("treatment", "code"))
            .limit(query.limit.coerceIn(1, 250))
            .into(mutableListOf())
            .map(MongoTaxMappers::taxProfileFromDocument)
    }
}
