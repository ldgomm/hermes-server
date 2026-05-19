package com.hermes.application.electronicinvoicing

import com.hermes.application.signature.ElectronicSignatureRepository
import com.hermes.domain.electronicinvoicing.*
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.signature.ElectronicSignature
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrganizationSriSettingsReadinessUseCaseTest {
    @Test
    fun `readiness passes when settings signature and sequence exist`() {
        val settingsRepository = InMemoryOrganizationSriSettingsRepository()
        val signatureRepository = OrganisationInMemorySignatureRepository()
        val sequenceRepository = InMemoryElectronicSequenceRepository12B()
        val clock = Clock.fixed(NOW, ZoneOffset.UTC)

        val settings = OrganizationSriSettings.create(
            organizationId = ORG,
            environment = SriEnvironment.TEST,
            ruc = "1790012345001",
            legalName = "Hermes Demo S.A.",
            commercialName = "Hermes",
            matrixAddress = "Av. Siempre Viva",
            establishmentAddress = "Local 1",
            establishmentCode = "001",
            emissionPointCode = "002",
            invoiceSchemaVersion = SriInvoiceSchemaVersion.V2_1_0,
            specialTaxpayerCode = null,
            obligatedToKeepAccounting = false,
            rimpeLegend = "CONTRIBUYENTE RÉGIMEN RIMPE",
            actorUserId = USER,
            now = NOW,
        )
        settingsRepository.save(settings)
        signatureRepository.create(
            ElectronicSignature.upload(
                id = "sig_1",
                organizationId = ORG,
                storageKey = "storage/key",
                passwordSecretRef = "secret/ref",
                subject = "CN=Hermes Demo",
                issuer = "CN=CA",
                validFrom = NOW.minusSeconds(3600),
                validTo = NOW.plusSeconds(86400 * 90),
                uploadedBy = USER,
                uploadedAt = NOW,
            ).markValidated(NOW)
        )
        sequenceRepository.createIfMissing(
            ElectronicSequence.create(
                id = "seq_1",
                organizationId = ORG,
                environment = SriEnvironment.TEST,
                documentType = SriDocumentType.INVOICE,
                series = SriSeries("001", "002"),
                startsAfter = 0,
                now = NOW,
            )
        )

        val result = CheckOrganizationSriReadinessUseCase(
            settingsRepository = settingsRepository,
            signatureRepository = signatureRepository,
            sequenceRepository = sequenceRepository,
            clock = clock,
        ).execute(
            CheckOrganizationSriReadinessCommand(
                organizationId = ORG,
                actorUserId = USER,
                actorEffectivePermissions = setOf(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS),
            )
        )

        assertTrue(result.ready)
        assertTrue(result.checks.all { it.ok })
    }

    @Test
    fun `readiness blocks when production is selected but gate is not enabled`() {
        val settingsRepository = InMemoryOrganizationSriSettingsRepository()
        val signatureRepository = OrganisationInMemorySignatureRepository()
        val sequenceRepository = InMemoryElectronicSequenceRepository12B()
        val clock = Clock.fixed(NOW, ZoneOffset.UTC)

        settingsRepository.save(
            OrganizationSriSettings.create(
                organizationId = ORG,
                environment = SriEnvironment.PRODUCTION,
                ruc = "1790012345001",
                legalName = "Hermes Demo S.A.",
                commercialName = null,
                matrixAddress = "Av. Siempre Viva",
                establishmentAddress = "Local 1",
                establishmentCode = "001",
                emissionPointCode = "002",
                invoiceSchemaVersion = SriInvoiceSchemaVersion.V2_1_0,
                specialTaxpayerCode = null,
                obligatedToKeepAccounting = false,
                rimpeLegend = null,
                actorUserId = USER,
                now = NOW,
            )
        )

        val result = CheckOrganizationSriReadinessUseCase(
            settingsRepository,
            signatureRepository,
            sequenceRepository,
            clock,
        ).execute(
            CheckOrganizationSriReadinessCommand(
                organizationId = ORG,
                actorUserId = USER,
                actorEffectivePermissions = setOf(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS),
            )
        )

        assertFalse(result.ready)
        assertTrue(result.checks.any { it.code == "production_gate_enabled" && !it.ok })
    }

    private companion object {
        const val ORG = "org_1"
        const val USER = "usr_1"
        val NOW: Instant = Instant.parse("2026-05-18T12:00:00Z")
    }
}

private class InMemoryOrganizationSriSettingsRepository : OrganizationSriSettingsRepository {
    private val values = mutableMapOf<String, OrganizationSriSettings>()
    override fun findByOrganizationId(organizationId: String): OrganizationSriSettings? = values[organizationId]
    override fun save(settings: OrganizationSriSettings): OrganizationSriSettings {
        values[settings.organizationId] = settings
        return settings
    }
}

private class OrganisationInMemorySignatureRepository : ElectronicSignatureRepository {
    private val values = mutableMapOf<String, ElectronicSignature>()
    override fun create(signature: ElectronicSignature) {
        values[signature.id] = signature
    }

    override fun update(signature: ElectronicSignature) {
        values[signature.id] = signature
    }

    override fun findById(id: String): ElectronicSignature? = values[id]
    override fun findActiveByOrganizationId(organizationId: String): ElectronicSignature? =
        values.values.firstOrNull { it.organizationId == organizationId && it.status.name == "VALID" }

    override fun findByOrganizationId(organizationId: String): List<ElectronicSignature> =
        values.values.filter { it.organizationId == organizationId }
}

private class InMemoryElectronicSequenceRepository12B : ElectronicSequenceRepository {
    private val values = mutableMapOf<String, ElectronicSequence>()
    override fun createIfMissing(sequence: ElectronicSequence): ElectronicSequence =
        values.getOrPut(sequence.key.storageKey) { sequence }

    override fun findByKey(key: ElectronicSequenceKey): ElectronicSequence? = values[key.storageKey]
    override fun nextSequential(command: NextElectronicSequentialCommand): ElectronicSequenceReservation {
        val key =
            ElectronicSequenceKey(command.organizationId, command.environment, command.documentType, command.series)
        val current = values[key.storageKey] ?: error("Missing sequence")
        val sequential = current.nextSequential()
        val updated = current.markIssued(sequential, command.documentId, command.issuedAt)
        values[key.storageKey] = updated
        return ElectronicSequenceReservation(updated, SriSequential(updated.currentValue))
    }
}
