package com.hermes.application.electronicinvoicing

import com.hermes.application.signature.ElectronicSignatureRepository
import com.hermes.domain.electronicinvoicing.ElectronicSequence
import com.hermes.domain.electronicinvoicing.ElectronicSequenceKey
import com.hermes.domain.electronicinvoicing.ElectronicSequenceReservation
import com.hermes.domain.electronicinvoicing.SriDocumentType
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriInvoiceSchemaVersion
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.signature.ElectronicSignature
import com.hermes.domain.signature.ElectronicSignatureStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ElectronicInvoiceHomologationProductionGateUseCasesTest {
    private val now = Instant.parse("2026-05-19T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `production gate blocks without confirmation phrase`() {
        val fixture = gateFixture()
        fixture.settingsRepository.save(settings(environment = SriEnvironment.PRODUCTION))
        fixture.signatureRepository.create(validSignature())
        fixture.sequenceRepository.createIfMissing(productionSequence())
        fixture.homologationRepository.create(approvedRun(now))

        val result = fixture.useCase.execute(
            EnableSriProductionCommand(
                organizationId = "org_test",
                actorUserId = "usr_admin",
                actorEffectivePermissions = setOf(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_ENABLE_PRODUCTION),
                confirmation = "WRONG",
                initialSequential = 1,
            )
        )

        assertFalse(result.enabled)
        assertTrue(result.checks.any { it.code == "confirmation_phrase" && !it.ok })
    }

    @Test
    fun `production gate enables only with homologation settings signature sequence flag and confirmation`() {
        val fixture = gateFixture()
        fixture.settingsRepository.save(settings(environment = SriEnvironment.PRODUCTION))
        fixture.signatureRepository.create(validSignature())
        fixture.sequenceRepository.createIfMissing(productionSequence())
        fixture.homologationRepository.create(approvedRun(now))

        val result = fixture.useCase.execute(
            EnableSriProductionCommand(
                organizationId = "org_test",
                actorUserId = "usr_admin",
                actorEffectivePermissions = setOf(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_ENABLE_PRODUCTION),
                confirmation = EnableSriProductionCommand.CONFIRMATION_PHRASE,
                initialSequential = 1,
            )
        )

        assertTrue(result.enabled)
        assertTrue(result.settings?.productionEnabled == true)
        assertTrue(result.checks.all { it.ok })
    }

    private fun gateFixture(): GateFixture12ETest {
        val settingsRepository = InMemory12ESriSettingsRepository()
        val signatureRepository = InMemory12EElectronicSignatureRepository()
        val sequenceRepository = InMemory12EElectronicSequenceRepository()
        val homologationRepository = InMemory12EHomologationRunRepository()
        val useCase = EnableSriProductionUseCase(
            settingsRepository = settingsRepository,
            signatureRepository = signatureRepository,
            sequenceRepository = sequenceRepository,
            homologationRunRepository = homologationRepository,
            endpointGateConfig = SriEndpointGateConfig(
                testReceptionWsdlUrl = "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl",
                testAuthorizationWsdlUrl = "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl",
                productionReceptionWsdlUrl = "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl",
                productionAuthorizationWsdlUrl = "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl",
                productionGloballyEnabled = true,
            ),
            clock = clock,
        )
        return GateFixture12ETest(
            settingsRepository,
            signatureRepository,
            sequenceRepository,
            homologationRepository,
            useCase
        )
    }

    private fun settings(environment: SriEnvironment): com.hermes.domain.electronicinvoicing.OrganizationSriSettings =
        com.hermes.domain.electronicinvoicing.OrganizationSriSettings.create(
            organizationId = "org_test",
            environment = environment,
            ruc = "1790012345001",
            legalName = "ALTOS DEL MURCO SA",
            commercialName = "Altos del Murco",
            matrixAddress = "Tambillo",
            establishmentAddress = "Tambillo",
            establishmentCode = "001",
            emissionPointCode = "002",
            invoiceSchemaVersion = SriInvoiceSchemaVersion.V2_1_0,
            specialTaxpayerCode = null,
            obligatedToKeepAccounting = false,
            rimpeLegend = null,
            actorUserId = "usr_admin",
            now = now,
        )

    private fun validSignature(): ElectronicSignature = ElectronicSignature.restore(
        id = "sig_001",
        organizationId = "org_test",
        storageKey = "encrypted/signatures/org_test/sig_001.p12",
        passwordSecretRef = "secret_sig_001",
        subject = "ALTOS DEL MURCO",
        issuer = "Test CA",
        validFrom = now.minusSeconds(3600),
        validTo = now.plusSeconds(86_400),
        status = ElectronicSignatureStatus.VALID,
        uploadedBy = "usr_admin",
        uploadedAt = now.minusSeconds(3600),
        lastUsedAt = null,
    )

    private fun productionSequence(): ElectronicSequence = ElectronicSequence.create(
        id = "seq_prod_001_002",
        organizationId = "org_test",
        environment = SriEnvironment.PRODUCTION,
        documentType = SriDocumentType.INVOICE,
        series = com.hermes.domain.electronicinvoicing.SriSeries("001", "002"),
        startsAfter = 0,
        now = now,
    )
}

private data class GateFixture12ETest(
    val settingsRepository: InMemory12ESriSettingsRepository,
    val signatureRepository: InMemory12EElectronicSignatureRepository,
    val sequenceRepository: InMemory12EElectronicSequenceRepository,
    val homologationRepository: InMemory12EHomologationRunRepository,
    val useCase: EnableSriProductionUseCase,
)

private class InMemory12ESriSettingsRepository : OrganizationSriSettingsRepository {
    private val values = mutableMapOf<String, com.hermes.domain.electronicinvoicing.OrganizationSriSettings>()
    override fun findByOrganizationId(organizationId: String) = values[organizationId]
    override fun save(settings: com.hermes.domain.electronicinvoicing.OrganizationSriSettings): com.hermes.domain.electronicinvoicing.OrganizationSriSettings {
        values[settings.organizationId] = settings
        return settings
    }
}

private class InMemory12EElectronicSignatureRepository : ElectronicSignatureRepository {
    private val values = mutableMapOf<String, ElectronicSignature>()
    override fun create(signature: ElectronicSignature) {
        values[signature.id] = signature
    }

    override fun update(signature: ElectronicSignature) {
        values[signature.id] = signature
    }

    override fun findById(id: String): ElectronicSignature? = values[id]
    override fun findActiveByOrganizationId(organizationId: String): ElectronicSignature? =
        values.values.firstOrNull { it.organizationId == organizationId && it.status == ElectronicSignatureStatus.VALID }

    override fun findByOrganizationId(organizationId: String): List<ElectronicSignature> =
        values.values.filter { it.organizationId == organizationId }
}

private class InMemory12EElectronicSequenceRepository : ElectronicSequenceRepository {
    private val values = mutableMapOf<String, ElectronicSequence>()
    override fun createIfMissing(sequence: ElectronicSequence): ElectronicSequence =
        values.getOrPut(sequence.key.storageKey) { sequence }

    override fun findByKey(key: ElectronicSequenceKey): ElectronicSequence? = values[key.storageKey]
    override fun nextSequential(command: NextElectronicSequentialCommand): ElectronicSequenceReservation {
        val key = ElectronicSequenceKey(
            organizationId = command.organizationId,
            environment = command.environment,
            documentType = command.documentType,
            series = command.series,
        )
        val current = values[key.storageKey] ?: error("missing sequence")
        val sequential = current.nextSequential()
        val updated = current.markIssued(sequential, command.documentId, command.issuedAt)
        values[key.storageKey] = updated
        return ElectronicSequenceReservation(updated, sequential)
    }
}

private class InMemory12EHomologationRunRepository : ElectronicInvoiceHomologationRunRepository {
    private val values = mutableMapOf<String, ElectronicInvoiceHomologationRun>()
    override fun create(run: ElectronicInvoiceHomologationRun) {
        values[run.id] = run
    }

    override fun findById(organizationId: String, runId: String): ElectronicInvoiceHomologationRun? =
        values[runId]?.takeIf { it.organizationId == organizationId }

    override fun search(query: ElectronicInvoiceHomologationRunSearchQuery): List<ElectronicInvoiceHomologationRun> =
        values.values.filter { it.organizationId == query.organizationId }.take(query.limit)

    override fun findLatestApprovedForProduction(organizationId: String): ElectronicInvoiceHomologationRun? =
        values.values.filter { it.organizationId == organizationId && it.approvedForProduction && it.status == ElectronicInvoiceHomologationRunStatus.PASSED }
            .maxByOrNull { it.createdAt }
}

private fun approvedRun(now: Instant): ElectronicInvoiceHomologationRun = ElectronicInvoiceHomologationRun(
    id = "homologation_run_001",
    organizationId = "org_test",
    status = ElectronicInvoiceHomologationRunStatus.PASSED,
    environment = SriEnvironment.TEST,
    requestedByUserId = "usr_admin",
    requiredScenarioCodes = ElectronicInvoiceHomologationScenarioCode.requiredForMvpProductionGate(),
    scenarioResults = emptyList(),
    reportMarkdown = "# Reporte aprobado",
    productionDecision = ElectronicInvoiceProductionReadinessDecision(
        approved = true,
        environment = SriEnvironment.TEST,
        reasons = listOf("All required scenarios passed."),
        decidedAt = now,
    ),
    approvedForProduction = true,
    startedAt = now.minusSeconds(60),
    finishedAt = now,
    createdAt = now,
    updatedAt = now,
)
