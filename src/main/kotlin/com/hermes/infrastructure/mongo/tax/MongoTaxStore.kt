package com.hermes.infrastructure.mongo.tax

import com.hermes.application.tax.OrganizationTaxSettingsRepository
import com.hermes.application.tax.TaxProfileRepository
import com.hermes.application.tax.TaxRateRepository
import com.hermes.domain.tax.OrganizationTaxSettings
import com.hermes.domain.tax.TaxProfile
import com.hermes.domain.tax.TaxProfileStatus
import com.hermes.domain.tax.TaxRate
import com.hermes.domain.tax.TaxRateStatus
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import org.bson.Document

/**
 * Aggregates the tax persistence adapters without implementing all tax repository
 * interfaces directly.
 *
 * Important:
 * TaxRateRepository and TaxProfileRepository both define:
 *
 * - findById(id: String)
 * - findByCode(code: String)
 * - findActive()
 *
 * Kotlin cannot safely implement both interfaces in one class because the JVM
 * signatures collide while the return types differ. Keep each repository as a
 * dedicated adapter and expose them through this store.
 */
class MongoTaxStore(
    database: MongoDatabase,
) {
    val rateRepository: TaxRateRepository = MongoTaxRateRepository(database)
    val profileRepository: TaxProfileRepository = MongoTaxProfileRepository(database)
    val settingsRepository: OrganizationTaxSettingsRepository = MongoOrganizationTaxSettingsRepository(database)
}

private class MongoTaxRateRepository(
    database: MongoDatabase,
) : TaxRateRepository {
    private val collection: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.TAX_RATES)

    override fun create(rate: TaxRate) {
        collection.insertOne(MongoTaxMappers.taxRateToDocument(rate))
    }

    override fun update(rate: TaxRate) {
        collection.replaceOne(
            eq("_id", rate.id),
            MongoTaxMappers.taxRateToDocument(rate),
            ReplaceOptions().upsert(false),
        )
    }

    override fun findById(id: String): TaxRate? =
        collection.find(eq("_id", id.trim()))
            .firstOrNull()
            ?.let(MongoTaxMappers::taxRateFromDocument)

    override fun findByCode(code: String): TaxRate? =
        collection.find(eq("code", code.trim()))
            .firstOrNull()
            ?.let(MongoTaxMappers::taxRateFromDocument)

    override fun findActive(): List<TaxRate> =
        collection.find(eq("status", TaxRateStatus.ACTIVE.name))
            .sort(Sorts.ascending("kind", "code"))
            .into(mutableListOf())
            .map(MongoTaxMappers::taxRateFromDocument)
}

private class MongoTaxProfileRepository(
    database: MongoDatabase,
) : TaxProfileRepository {
    private val collection: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.TAX_PROFILES)

    override fun create(profile: TaxProfile) {
        collection.insertOne(MongoTaxMappers.taxProfileToDocument(profile))
    }

    override fun update(profile: TaxProfile) {
        collection.replaceOne(
            eq("_id", profile.id),
            MongoTaxMappers.taxProfileToDocument(profile),
            ReplaceOptions().upsert(false),
        )
    }

    override fun findById(id: String): TaxProfile? =
        collection.find(eq("_id", id.trim()))
            .firstOrNull()
            ?.let(MongoTaxMappers::taxProfileFromDocument)

    override fun findByCode(code: String): TaxProfile? =
        collection.find(eq("code", code.trim()))
            .firstOrNull()
            ?.let(MongoTaxMappers::taxProfileFromDocument)

    override fun findActive(): List<TaxProfile> =
        collection.find(eq("status", TaxProfileStatus.ACTIVE.name))
            .sort(Sorts.ascending("treatment", "code"))
            .into(mutableListOf())
            .map(MongoTaxMappers::taxProfileFromDocument)
}

private class MongoOrganizationTaxSettingsRepository(
    database: MongoDatabase,
) : OrganizationTaxSettingsRepository {
    private val collection: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.ORGANIZATION_TAX_SETTINGS)

    override fun create(settings: OrganizationTaxSettings) {
        collection.insertOne(MongoTaxMappers.organizationTaxSettingsToDocument(settings))
    }

    override fun update(settings: OrganizationTaxSettings) {
        collection.replaceOne(
            eq("organizationId", settings.organizationId),
            MongoTaxMappers.organizationTaxSettingsToDocument(settings),
            ReplaceOptions().upsert(false),
        )
    }

    override fun findByOrganizationId(organizationId: String): OrganizationTaxSettings? =
        collection.find(eq("organizationId", organizationId.trim()))
            .firstOrNull()
            ?.let(MongoTaxMappers::organizationTaxSettingsFromDocument)
}
