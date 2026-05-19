package com.hermes.backend.electronicinvoicing

import com.hermes.application.electronicinvoicing.CheckOrganizationSriReadinessUseCase
import com.hermes.application.electronicinvoicing.DefaultElectronicSignatureVault
import com.hermes.application.electronicinvoicing.SimpleSriRidePdfRenderer
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceTimelineUseCase
import com.hermes.application.electronicinvoicing.GenerateElectronicInvoiceRideUseCase
import com.hermes.application.electronicinvoicing.EmailElectronicInvoiceUseCase
import com.hermes.application.electronicinvoicing.DownloadElectronicInvoiceArtifactUseCase
import com.hermes.application.electronicinvoicing.SriEndpointGateConfig
import com.hermes.application.electronicinvoicing.RunElectronicInvoiceHomologationUseCase
import com.hermes.application.electronicinvoicing.RunElectronicInvoiceHomologationFromAdminUseCase
import com.hermes.application.electronicinvoicing.RetrySriAuthorizationUseCaseRunner
import com.hermes.application.electronicinvoicing.RetrySriAuthorizationUseCase
import com.hermes.application.electronicinvoicing.ListElectronicInvoiceHomologationRunsUseCase
import com.hermes.application.electronicinvoicing.IssueElectronicInvoiceUseCaseRunner
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceHomologationRunUseCase
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceHomologationReportUseCase
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceHomologationReadinessUseCase
import com.hermes.application.electronicinvoicing.EnableSriProductionUseCase
import com.hermes.application.electronicinvoicing.EmailElectronicInvoiceUseCaseDeliveryRunner
import com.hermes.application.electronicinvoicing.ApproveElectronicInvoiceProductionReadinessUseCase
import com.hermes.application.electronicinvoicing.EnsureElectronicSequenceAdminUseCase
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceErrorsUseCase
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceUseCase
import com.hermes.application.electronicinvoicing.GetElectronicSequenceUseCase
import com.hermes.application.electronicinvoicing.GetOrganizationSriSettingsUseCase
import com.hermes.application.electronicinvoicing.IssueElectronicInvoiceFromSaleUseCase
import com.hermes.application.electronicinvoicing.IssueElectronicInvoiceUseCase
import com.hermes.application.electronicinvoicing.ListElectronicInvoicesUseCase
import com.hermes.application.electronicinvoicing.ListElectronicSequencesUseCase
import com.hermes.application.electronicinvoicing.QuerySriAuthorizationUseCase
import com.hermes.application.electronicinvoicing.ReserveSriAccessKeyUseCase
import com.hermes.application.electronicinvoicing.RetryElectronicInvoiceAuthorizationUseCase
import com.hermes.application.electronicinvoicing.SaleBackedElectronicInvoiceXmlCommandFactory
import com.hermes.application.electronicinvoicing.SignElectronicDocumentUseCase
import com.hermes.application.electronicinvoicing.SignElectronicDocumentUseCaseSigningService
import com.hermes.application.electronicinvoicing.SubmitElectronicDocumentToSriUseCase
import com.hermes.application.electronicinvoicing.UpsertOrganizationSriSettingsUseCase
import com.hermes.application.electronicinvoicing.UuidElectronicInvoiceIssueIdGenerator
import com.hermes.application.signature.ActivateElectronicSignatureUseCase
import com.hermes.application.signature.GetElectronicSignatureUseCase
import com.hermes.application.signature.ListElectronicSignaturesUseCase
import com.hermes.application.signature.RevokeElectronicSignatureUseCase
import com.hermes.application.signature.UploadElectronicSignatureUseCase
import com.hermes.application.signature.ValidateElectronicSignatureUseCase
import com.hermes.infrastructure.mongo.electronicinvoicing.MongoElectronicDocumentArtifactStorage
import com.hermes.infrastructure.mongo.electronicinvoicing.MongoElectronicInvoiceIssueAuditLogger
import com.hermes.infrastructure.mongo.electronicinvoicing.MongoElectronicInvoiceHomologationRunRepository
import com.hermes.infrastructure.mongo.electronicinvoicing.MongoElectronicInvoiceIssueRepository
import com.hermes.infrastructure.mongo.electronicinvoicing.MongoElectronicInvoiceIssueTimelineRepository
import com.hermes.infrastructure.mongo.electronicinvoicing.MongoElectronicSequenceRepository
import com.hermes.infrastructure.mongo.electronicinvoicing.MongoOrganizationSriSettingsRepository
import com.hermes.infrastructure.mongo.sales.MongoSalesStore
import com.hermes.infrastructure.mongo.signature.MongoElectronicSignatureRepository
import com.hermes.infrastructure.security.LocalEncryptedSignatureSecretStore
import com.hermes.infrastructure.security.Pkcs12CertificateInspector
import com.hermes.infrastructure.security.Pkcs12SigningKeyMaterialLoader
import com.hermes.infrastructure.sri.JdkSriSoapTransport
import com.hermes.infrastructure.sri.SoapSriAuthorizationClient
import com.hermes.infrastructure.sri.SoapSriReceptionClient
import com.hermes.infrastructure.sri.SriSoapTransport
import com.hermes.infrastructure.sri.SriWsConfig
import com.hermes.infrastructure.xml.JaxpSriXsdValidator
import com.hermes.infrastructure.xml.SriInvoiceXmlBuilder
import com.hermes.infrastructure.xml.XadesBesXmlSigner
import com.mongodb.client.MongoDatabase
import java.nio.file.Path
import java.time.Clock

object ElectronicInvoicingModuleFactory {
    fun fromMongo(
        database: MongoDatabase,
        artifactRoot: Path = Path.of("build/hermes-electronic-invoicing"),
        sriWsConfig: SriWsConfig = SriWsConfig(),
        sriSoapTransport: SriSoapTransport = JdkSriSoapTransport(),
        clock: Clock = Clock.systemUTC(),
        sriProductionGloballyEnabled: Boolean = false,
    ): ElectronicInvoicingModule {
        val issueRepository = MongoElectronicInvoiceIssueRepository(database)
        val settingsRepository = MongoOrganizationSriSettingsRepository(database)
        val sequenceRepository = MongoElectronicSequenceRepository(database)
        val signatureRepository = MongoElectronicSignatureRepository(database)
        val salesStore = MongoSalesStore(database)
        val signatureSecretStore = LocalEncryptedSignatureSecretStore.default()
        val certificateInspector = Pkcs12CertificateInspector()
        val artifactStorage = MongoElectronicDocumentArtifactStorage(database, artifactRoot)
        val issueAuditLogger = MongoElectronicInvoiceIssueAuditLogger(database)
        val timelineRepository = MongoElectronicInvoiceIssueTimelineRepository(database)
        val homologationRunRepository = MongoElectronicInvoiceHomologationRunRepository(database)

        val receptionClient = SoapSriReceptionClient(
            config = sriWsConfig,
            transport = sriSoapTransport,
        )
        val authorizationClient = SoapSriAuthorizationClient(
            config = sriWsConfig,
            transport = sriSoapTransport,
        )

        val querySriAuthorizationUseCase = QuerySriAuthorizationUseCase(
            repository = issueRepository,
            artifactStorage = artifactStorage,
            authorizationClient = authorizationClient,
            auditLogger = issueAuditLogger,
            clock = clock,
        )
        val submitElectronicDocumentToSriUseCase = SubmitElectronicDocumentToSriUseCase(
            repository = issueRepository,
            artifactStorage = artifactStorage,
            receptionClient = receptionClient,
            auditLogger = issueAuditLogger,
            clock = clock,
        )
        val signatureVault = DefaultElectronicSignatureVault(
            signatureRepository = signatureRepository,
            secretReader = signatureSecretStore,
            keyMaterialLoader = Pkcs12SigningKeyMaterialLoader(),
        )
        val signElectronicDocumentUseCase = SignElectronicDocumentUseCase(
            signatureVault = signatureVault,
            signatureRepository = signatureRepository,
            xmlSigner = XadesBesXmlSigner(),
            clock = clock,
        )
        val issueElectronicInvoiceUseCase = IssueElectronicInvoiceUseCase(
            idGenerator = UuidElectronicInvoiceIssueIdGenerator(),
            accessKeyUseCase = ReserveSriAccessKeyUseCase(sequenceRepository),
            xmlCommandFactory = SaleBackedElectronicInvoiceXmlCommandFactory(
                saleRepository = salesStore.saleRepository,
                settingsRepository = settingsRepository,
            ),
            xmlBuilder = SriInvoiceXmlBuilder(),
            xsdValidator = JaxpSriXsdValidator(),
            signingService = SignElectronicDocumentUseCaseSigningService(signElectronicDocumentUseCase),
            submitToSriUseCase = submitElectronicDocumentToSriUseCase,
            queryAuthorizationUseCase = querySriAuthorizationUseCase,
            repository = issueRepository,
            artifactStorage = artifactStorage,
            auditLogger = issueAuditLogger,
            clock = clock,
        )

        val generateElectronicInvoiceRideUseCase = GenerateElectronicInvoiceRideUseCase(
            repository = issueRepository,
            artifactStorage = artifactStorage,
            artifactReader = artifactStorage,
            rideRenderer = SimpleSriRidePdfRenderer(),
            auditLogger = issueAuditLogger,
            clock = clock,
        )
        val emailElectronicInvoiceUseCase = EmailElectronicInvoiceUseCase(
            repository = issueRepository,
            artifactReader = artifactStorage,
            generateRideUseCase = generateElectronicInvoiceRideUseCase,
            auditLogger = issueAuditLogger,
            clock = clock,
        )

        val endpointGateConfig = SriEndpointGateConfig(
            testReceptionWsdlUrl = sriWsConfig.testReceptionUrl,
            testAuthorizationWsdlUrl = sriWsConfig.testAuthorizationUrl,
            productionReceptionWsdlUrl = sriWsConfig.productionReceptionUrl,
            productionAuthorizationWsdlUrl = sriWsConfig.productionAuthorizationUrl,
            productionGloballyEnabled = sriProductionGloballyEnabled,
        )
        val homologationUseCase = RunElectronicInvoiceHomologationUseCase(
            issueRunner = IssueElectronicInvoiceUseCaseRunner(issueElectronicInvoiceUseCase),
            retryAuthorizationRunner = RetrySriAuthorizationUseCaseRunner(
                RetrySriAuthorizationUseCase(querySriAuthorizationUseCase)
            ),
            deliveryRunner = EmailElectronicInvoiceUseCaseDeliveryRunner(emailElectronicInvoiceUseCase),
            clock = clock,
        )

        return ElectronicInvoicingModule(
            getElectronicInvoiceUseCase = GetElectronicInvoiceUseCase(issueRepository),
            listElectronicInvoicesUseCase = ListElectronicInvoicesUseCase(issueRepository),
            getOrganizationSriSettingsUseCase = GetOrganizationSriSettingsUseCase(settingsRepository),
            upsertOrganizationSriSettingsUseCase = UpsertOrganizationSriSettingsUseCase(settingsRepository),
            checkOrganizationSriReadinessUseCase = CheckOrganizationSriReadinessUseCase(
                settingsRepository = settingsRepository,
                signatureRepository = signatureRepository,
                sequenceRepository = sequenceRepository,
            ),
            uploadElectronicSignatureUseCase = UploadElectronicSignatureUseCase(
                repository = signatureRepository,
                secretStore = signatureSecretStore,
                inspector = certificateInspector,
            ),
            listElectronicSignaturesUseCase = ListElectronicSignaturesUseCase(signatureRepository),
            getElectronicSignatureUseCase = GetElectronicSignatureUseCase(signatureRepository),
            validateElectronicSignatureUseCase = ValidateElectronicSignatureUseCase(
                repository = signatureRepository,
                secretReader = signatureSecretStore,
                inspector = certificateInspector,
            ),
            activateElectronicSignatureUseCase = ActivateElectronicSignatureUseCase(signatureRepository),
            revokeElectronicSignatureUseCase = RevokeElectronicSignatureUseCase(signatureRepository),
            ensureElectronicSequenceAdminUseCase = EnsureElectronicSequenceAdminUseCase(sequenceRepository),
            listElectronicSequencesUseCase = ListElectronicSequencesUseCase(sequenceRepository),
            getElectronicSequenceUseCase = GetElectronicSequenceUseCase(sequenceRepository),
            issueElectronicInvoiceFromSaleUseCase = IssueElectronicInvoiceFromSaleUseCase(
                saleRepository = salesStore.saleRepository,
                settingsRepository = settingsRepository,
                signatureRepository = signatureRepository,
                sequenceRepository = sequenceRepository,
                issueElectronicInvoiceUseCase = issueElectronicInvoiceUseCase,
                clock = clock,
            ),
            retryElectronicInvoiceAuthorizationUseCase = RetryElectronicInvoiceAuthorizationUseCase(
                issueRepository = issueRepository,
                querySriAuthorizationUseCase = querySriAuthorizationUseCase,
            ),
            getElectronicInvoiceErrorsUseCase = GetElectronicInvoiceErrorsUseCase(issueRepository),
            generateElectronicInvoiceRideUseCase = generateElectronicInvoiceRideUseCase,
            emailElectronicInvoiceUseCase = emailElectronicInvoiceUseCase,
            downloadElectronicInvoiceArtifactUseCase = DownloadElectronicInvoiceArtifactUseCase(
                issueRepository = issueRepository,
                artifactReader = artifactStorage,
            ),
            getElectronicInvoiceTimelineUseCase = GetElectronicInvoiceTimelineUseCase(
                issueRepository = issueRepository,
                timelineRepository = timelineRepository,
            ),
            getElectronicInvoiceHomologationReadinessUseCase = GetElectronicInvoiceHomologationReadinessUseCase(
                settingsRepository = settingsRepository,
                signatureRepository = signatureRepository,
                endpointGateConfig = endpointGateConfig,
            ),
            runElectronicInvoiceHomologationFromAdminUseCase = RunElectronicInvoiceHomologationFromAdminUseCase(
                settingsRepository = settingsRepository,
                signatureRepository = signatureRepository,
                endpointGateConfig = endpointGateConfig,
                homologationUseCase = homologationUseCase,
                approvalUseCase = ApproveElectronicInvoiceProductionReadinessUseCase(clock),
                repository = homologationRunRepository,
                clock = clock,
            ),
            listElectronicInvoiceHomologationRunsUseCase = ListElectronicInvoiceHomologationRunsUseCase(
                repository = homologationRunRepository,
            ),
            getElectronicInvoiceHomologationRunUseCase = GetElectronicInvoiceHomologationRunUseCase(
                repository = homologationRunRepository,
            ),
            getElectronicInvoiceHomologationReportUseCase = GetElectronicInvoiceHomologationReportUseCase(
                repository = homologationRunRepository,
            ),
            enableSriProductionUseCase = EnableSriProductionUseCase(
                settingsRepository = settingsRepository,
                signatureRepository = signatureRepository,
                sequenceRepository = sequenceRepository,
                homologationRunRepository = homologationRunRepository,
                endpointGateConfig = endpointGateConfig,
                clock = clock,
            ),
        )
    }
}
