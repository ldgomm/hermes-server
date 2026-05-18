package com.hermes.infrastructure.mongo.tax

import com.hermes.domain.tax.EcuadorTaxSeed
import com.hermes.domain.tax.OrganizationTaxSettings
import com.hermes.domain.tax.OrganizationTaxSettingsStatus
import com.hermes.domain.tax.TaxRegimeCode
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.core.M008CreateTaxEngineMigration
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.client.MongoClient
import org.bson.Document
import org.bson.types.Decimal128
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class MongoTaxStoreTest {
    private lateinit var client: MongoClient
    private lateinit var databaseName: String

    @BeforeEach
    fun setUp() {
        MongoIntegrationTestSupport.assumeMongoAvailable()
        client = MongoIntegrationTestSupport.client()
        databaseName = MongoIntegrationTestSupport.databaseName("hermes_tax_store_test")
        M008CreateTaxEngineMigration.up(client.getDatabase(databaseName))
    }

    @AfterEach
    fun tearDown() {
        if (::client.isInitialized) {
            runCatching { client.getDatabase(databaseName).drop() }
            runCatching { client.close() }
        }
    }

    @Test
    fun `stores and reads tax rates preserving Decimal128 precision`() {
        val database = client.getDatabase(databaseName)
        val store = MongoTaxStore(database)
        val rateRepository = store.rateRepository
        val rate = EcuadorTaxSeed.rates.first()

        rateRepository.create(rate)

        val persisted = rateRepository.findByCode(rate.code)

        assertNotNull(persisted)
        assertEquals(rate.id, persisted!!.id)
        assertEquals(rate.code, persisted.code)
        assertEquals(rate.rate, persisted.rate)
        assertEquals(rate.status, persisted.status)

        val raw = database.getCollection(MongoCollectionNames.TAX_RATES)
            .find(Document("code", rate.code))
            .first() ?: error("Expected persisted tax rate document.")

        assertTrue(raw["rate"] is Decimal128)
    }

    @Test
    fun `stores and reads tax profiles with embedded rate`() {
        val database = client.getDatabase(databaseName)
        val store = MongoTaxStore(database)
        val profileRepository = store.profileRepository
        val profile = EcuadorTaxSeed.profiles.first { it.taxRate != null }

        profileRepository.create(profile)

        val persisted = profileRepository.findByCode(profile.code)

        assertNotNull(persisted)
        assertEquals(profile.id, persisted!!.id)
        assertEquals(profile.code, persisted.code)
        assertEquals(profile.treatment, persisted.treatment)
        assertNotNull(persisted.taxRate)
        assertEquals(profile.taxRate!!.code, persisted.taxRate!!.code)
        assertEquals(profile.taxRate.rate, persisted.taxRate.rate)
    }

    @Test
    fun `stores and reads tax profiles without tax rate`() {
        val database = client.getDatabase(databaseName)
        val store = MongoTaxStore(database)
        val profileRepository = store.profileRepository
        val profile = EcuadorTaxSeed.profiles.first { it.taxRate == null }

        profileRepository.create(profile)

        val persisted = profileRepository.findByCode(profile.code)

        assertNotNull(persisted)
        assertEquals(profile.id, persisted!!.id)
        assertEquals(profile.code, persisted.code)
        assertEquals(profile.treatment, persisted.treatment)
        assertNull(persisted.taxRate)
    }

    @Test
    fun `stores and reads organization tax settings`() {
        val database = client.getDatabase(databaseName)
        val store = MongoTaxStore(database)
        val settingsRepository = store.settingsRepository
        val now = Instant.parse("2026-05-17T12:00:00Z")
        val settings = OrganizationTaxSettings(
            id = "taxset_org_test",
            organizationId = "org_test",
            regime = TaxRegimeCode.RIMPE_ENTREPRENEUR,
            defaultTaxProfileCode = "iva_current_full",
            enabledTaxProfileCodes = setOf(
                "iva_current_full",
                "iva_0",
                "exempt_iva",
                "not_subject_to_iva",
                "no_tax_internal",
            ),
            allowTaxInclusivePrices = true,
            allowManualLineDiscounts = true,
            requireTaxProfileForCatalogItems = true,
            status = OrganizationTaxSettingsStatus.ACTIVE,
            createdAt = now,
            updatedAt = now,
            createdBy = "usr_owner",
            updatedBy = "usr_owner",
        )

        settingsRepository.create(settings)

        val persisted = settingsRepository.findByOrganizationId("org_test")

        assertNotNull(persisted)
        assertEquals(settings.id, persisted!!.id)
        assertEquals(settings.organizationId, persisted.organizationId)
        assertEquals(settings.regime, persisted.regime)
        assertEquals(settings.defaultTaxProfileCode, persisted.defaultTaxProfileCode)
        assertTrue("iva_0" in persisted.enabledTaxProfileCodes)
        assertTrue(persisted.allowTaxInclusivePrices)
        assertTrue(persisted.allowManualLineDiscounts)
        assertTrue(persisted.requireTaxProfileForCatalogItems)
    }

    @Test
    fun `find active returns only active rates and profiles`() {
        val database = client.getDatabase(databaseName)
        val store = MongoTaxStore(database)

        EcuadorTaxSeed.rates.forEach(store.rateRepository::create)
        EcuadorTaxSeed.profiles.forEach(store.profileRepository::create)

        val activeRates = store.rateRepository.findActive()
        val activeProfiles = store.profileRepository.findActive()

        assertTrue(activeRates.isNotEmpty())
        assertTrue(activeProfiles.isNotEmpty())
        assertTrue(activeRates.all { it.status.name == "ACTIVE" })
        assertTrue(activeProfiles.all { it.status.name == "ACTIVE" })
    }
}
