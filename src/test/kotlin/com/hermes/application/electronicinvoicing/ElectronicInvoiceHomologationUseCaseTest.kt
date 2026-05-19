package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.*
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ElectronicInvoiceHomologationUseCaseTest {
    private val now: Instant = Instant.parse("2026-05-18T17:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `readiness fails when environment or endpoints are not from SRI test`() {
        val result = CheckElectronicInvoiceHomologationReadinessUseCase(clock).execute(
            ElectronicInvoiceHomologationReadinessCommand(
                organizationId = ORG,
                actorUserId = USER,
                environment = SriEnvironment.PRODUCTION,
                issuerRuc = RUC,
                series = SriSeries("001", "001"),
                schemaVersionCode = "2.1.0",
                endpoints = ElectronicInvoiceHomologationEndpointConfig(
                    receptionWsdlUrl = "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl",
                    authorizationWsdlUrl = "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl",
                ),
                activeSignatureId = "sig_1",
            )
        )

        assertFalse(result.ready)
        assertTrue(result.failedChecks.any { it.id == "environment_is_test" })
        assertTrue(result.failedChecks.any { it.id == "endpoints_are_test" })
    }

    @Test
    fun `runs required scenarios and approves production readiness`() {
        val issueRunner = HomologationFakeIssueRunner()
        val deliveryRunner = HomologationFakeDeliveryRunner()
        val useCase = RunElectronicInvoiceHomologationUseCase(
            issueRunner = issueRunner,
            deliveryRunner = deliveryRunner,
            clock = clock,
        )

        val required = setOf(
            ElectronicInvoiceHomologationScenarioCode.FINAL_CONSUMER,
            ElectronicInvoiceHomologationScenarioCode.EMAIL_DELIVERY_WITH_RIDE,
        )
        val report = useCase.execute(
            RunElectronicInvoiceHomologationCommand(
                organizationId = ORG,
                actorUserId = USER,
                environment = SriEnvironment.TEST,
                requiredScenarioCodes = required,
                scenarios = listOf(
                    scenario(
                        code = ElectronicInvoiceHomologationScenarioCode.FINAL_CONSUMER,
                        documentId = "doc_final_consumer",
                        saleId = "sale_final_consumer",
                        expectedFinalStatus = ElectronicDocumentStatus.AUTHORIZED,
                    ),
                    scenario(
                        code = ElectronicInvoiceHomologationScenarioCode.EMAIL_DELIVERY_WITH_RIDE,
                        documentId = "doc_email",
                        saleId = "sale_email",
                        expectedFinalStatus = ElectronicDocumentStatus.DELIVERED,
                        emailTo = "cliente@example.com",
                    ),
                ),
            )
        )

        assertTrue(report.passed)
        assertEquals(2, report.scenarioResults.size)
        assertTrue(report.scenarioResults.all { it.passed })
        assertEquals(ElectronicDocumentStatus.DELIVERED, report.scenarioResults.last().finalDocumentStatus)

        val decision = ApproveElectronicInvoiceProductionReadinessUseCase(clock).execute(report)
        assertTrue(decision.approved)
    }

    @Test
    fun `strict homologation rejects missing required scenarios before executing`() {
        val useCase = RunElectronicInvoiceHomologationUseCase(
            issueRunner = HomologationFakeIssueRunner(),
            clock = clock,
        )

        val result = kotlin.runCatching {
            useCase.execute(
                RunElectronicInvoiceHomologationCommand(
                    organizationId = ORG,
                    actorUserId = USER,
                    environment = SriEnvironment.TEST,
                    requiredScenarioCodes = setOf(
                        ElectronicInvoiceHomologationScenarioCode.FINAL_CONSUMER,
                        ElectronicInvoiceHomologationScenarioCode.RUC_BUYER,
                    ),
                    scenarios = listOf(
                        scenario(
                            code = ElectronicInvoiceHomologationScenarioCode.FINAL_CONSUMER,
                            documentId = "doc_1",
                            saleId = "sale_1",
                        )
                    ),
                )
            )
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Missing required SRI homologation scenarios"))
    }

    @Test
    fun `PPR scenario retries authorization and keeps same access key`() {
        val issueRunner = HomologationFakeIssueRunner(firstStatus = ElectronicDocumentStatus.AUTHORIZATION_PENDING)
        val retryRunner = HomologationFakeRetryRunner()
        val useCase = RunElectronicInvoiceHomologationUseCase(
            issueRunner = issueRunner,
            retryAuthorizationRunner = retryRunner,
            clock = clock,
        )

        val report = useCase.execute(
            RunElectronicInvoiceHomologationCommand(
                organizationId = ORG,
                actorUserId = USER,
                environment = SriEnvironment.TEST,
                requiredScenarioCodes = setOf(ElectronicInvoiceHomologationScenarioCode.AUTHORIZATION_PROCESSING_RETRY),
                scenarios = listOf(
                    scenario(
                        code = ElectronicInvoiceHomologationScenarioCode.AUTHORIZATION_PROCESSING_RETRY,
                        documentId = "doc_ppr",
                        saleId = "sale_ppr",
                        expectedFinalStatus = ElectronicDocumentStatus.AUTHORIZED,
                        retryAuthorizationWhenPending = true,
                    )
                ),
            )
        )

        assertTrue(report.passed)
        assertEquals(issueRunner.lastAccessKey, retryRunner.lastReceivedAccessKey)
    }

    private fun scenario(
        code: ElectronicInvoiceHomologationScenarioCode,
        documentId: String,
        saleId: String,
        expectedFinalStatus: ElectronicDocumentStatus = ElectronicDocumentStatus.AUTHORIZED,
        retryAuthorizationWhenPending: Boolean = false,
        emailTo: String? = null,
    ): ElectronicInvoiceHomologationScenarioCommand = ElectronicInvoiceHomologationScenarioCommand(
        code = code,
        issueCommand = IssueElectronicInvoiceCommand(
            organizationId = ORG,
            actorUserId = USER,
            saleId = saleId,
            branchId = BRANCH,
            emissionPointId = EMISSION_POINT,
            environment = SriEnvironment.TEST,
            issuerRuc = RUC,
            series = SriSeries("001", "001"),
            issuedDate = LocalDate.of(2026, 5, 18),
            numericCode = SriNumericCode("12345678"),
            documentId = documentId,
            issuedAt = now,
        ),
        expectedFinalStatus = expectedFinalStatus,
        retryAuthorizationWhenPending = retryAuthorizationWhenPending,
        emailTo = emailTo,
    )

    private inner class HomologationFakeIssueRunner(
        private val firstStatus: ElectronicDocumentStatus = ElectronicDocumentStatus.AUTHORIZED,
    ) : ElectronicInvoiceIssueRunner {
        var lastAccessKey: String? = null

        override fun issue(command: IssueElectronicInvoiceCommand): ElectronicInvoiceIssueOutcome {
            val record = record(command.documentId!!, command.saleId, firstStatus)
            lastAccessKey = record.accessKey.value
            val artifacts = if (firstStatus == ElectronicDocumentStatus.AUTHORIZATION_PENDING) {
                emptyList()
            } else {
                listOf(artifact(record.id, ElectronicDocumentArtifactType.AUTHORIZED_XML))
            }
            return ElectronicInvoiceIssueOutcome(
                record = record,
                generatedXmlPresent = true,
                validationValid = true,
                signedXmlPresent = firstStatus != ElectronicDocumentStatus.XSD_INVALID,
                receptionReceived = true,
                authorizationStatus = if (firstStatus == ElectronicDocumentStatus.AUTHORIZATION_PENDING) {
                    SriAuthorizationStatus.PROCESSING
                } else {
                    SriAuthorizationStatus.AUTHORIZED
                },
                authorized = firstStatus == ElectronicDocumentStatus.AUTHORIZED,
                artifacts = artifacts,
            )
        }
    }

    private inner class HomologationFakeRetryRunner : ElectronicInvoiceAuthorizationRetryRunner {
        var lastReceivedAccessKey: String? = null

        override fun retry(command: QuerySriAuthorizationCommand): ElectronicInvoiceAuthorizationRetryOutcome {
            lastReceivedAccessKey = command.record.accessKey.value
            val authorized = command.record.copy(
                status = ElectronicDocumentStatus.AUTHORIZED,
                authorizedAt = now,
                updatedAt = now,
            )
            return ElectronicInvoiceAuthorizationRetryOutcome(
                record = authorized,
                authorizationStatus = SriAuthorizationStatus.AUTHORIZED,
                authorized = true,
                artifacts = listOf(artifact(authorized.id, ElectronicDocumentArtifactType.AUTHORIZED_XML)),
            )
        }
    }

    private inner class HomologationFakeDeliveryRunner : ElectronicInvoiceDeliveryRunner {
        override fun deliver(command: EmailElectronicInvoiceCommand): ElectronicInvoiceDeliveryOutcome {
            val issued = record(command.documentId, "sale_email", ElectronicDocumentStatus.DELIVERED).copy(
                deliveryEmailTo = command.emailTo,
                updatedAt = now,
            )
            return ElectronicInvoiceDeliveryOutcome(record = issued, delivered = true)
        }
    }

    private fun artifact(documentId: String, type: ElectronicDocumentArtifactType): StoredElectronicDocumentArtifact =
        StoredElectronicDocumentArtifact(
            objectKey = "electronic/$ORG/$documentId/${type.storageValue}.xml",
            artifactType = type,
            sha256 = "a".repeat(64),
            sizeBytes = 128,
            createdAt = now,
        )

    private fun record(id: String, saleId: String, status: ElectronicDocumentStatus): ElectronicInvoiceIssueRecord {
        val series = SriSeries("001", "001")
        val accessKey = SriAccessKeyGenerator.generate(
            SriAccessKeyGenerationCommand(
                issuedDate = LocalDate.of(2026, 5, 18),
                documentType = SriDocumentType.INVOICE,
                ruc = RUC,
                environment = SriEnvironment.TEST,
                series = series,
                sequential = SriSequential(1),
                numericCode = SriNumericCode("12345678"),
            )
        )
        return ElectronicInvoiceIssueRecord(
            id = id,
            organizationId = ORG,
            branchId = BRANCH,
            emissionPointId = EMISSION_POINT,
            saleId = saleId,
            environment = SriEnvironment.TEST,
            documentType = SriDocumentType.INVOICE,
            series = series,
            documentNumber = "001-001-000000001",
            accessKey = accessKey,
            authorizationNumber = accessKey.value,
            status = status,
            signedXmlObjectKey = "signed.xml",
            signedXmlSha256 = "a".repeat(64),
            authorizedXmlObjectKey = if (status in setOf(
                    ElectronicDocumentStatus.AUTHORIZED,
                    ElectronicDocumentStatus.DELIVERY_PENDING,
                    ElectronicDocumentStatus.DELIVERED
                )
            ) "authorized.xml" else null,
            authorizedXmlSha256 = if (status in setOf(
                    ElectronicDocumentStatus.AUTHORIZED,
                    ElectronicDocumentStatus.DELIVERY_PENDING,
                    ElectronicDocumentStatus.DELIVERED
                )
            ) "b".repeat(64) else null,
            issuedAt = now,
            authorizedAt = if (status in setOf(
                    ElectronicDocumentStatus.AUTHORIZED,
                    ElectronicDocumentStatus.DELIVERY_PENDING,
                    ElectronicDocumentStatus.DELIVERED
                )
            ) now else null,
            createdAt = now,
            updatedAt = now,
            createdBy = USER,
            updatedBy = USER,
        )
    }

    private companion object {
        const val ORG = "org_1"
        const val USER = "usr_1"
        const val BRANCH = "br_1"
        const val EMISSION_POINT = "emi_1"
        const val RUC = "1790012345001"
    }
}
