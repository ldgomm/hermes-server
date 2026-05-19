package com.hermes.application.electronicinvoicing

interface ElectronicInvoiceIssueRunner {
    fun issue(command: IssueElectronicInvoiceCommand): ElectronicInvoiceIssueOutcome
}

class IssueElectronicInvoiceUseCaseRunner(
    private val useCase: IssueElectronicInvoiceUseCase,
) : ElectronicInvoiceIssueRunner {
    override fun issue(command: IssueElectronicInvoiceCommand): ElectronicInvoiceIssueOutcome {
        val result = useCase.execute(command)
        return ElectronicInvoiceIssueOutcome(
            record = result.record,
            generatedXmlPresent = result.generatedXml != null,
            validationValid = result.validation?.valid,
            signedXmlPresent = result.signedXml != null,
            receptionReceived = result.reception?.canQueryAuthorization == true,
            authorizationStatus = result.authorization?.status,
            authorized = result.authorized,
            artifacts = result.artifacts,
        )
    }
}

interface ElectronicInvoiceAuthorizationRetryRunner {
    fun retry(command: QuerySriAuthorizationCommand): ElectronicInvoiceAuthorizationRetryOutcome
}

class RetrySriAuthorizationUseCaseRunner(
    private val useCase: RetrySriAuthorizationUseCase,
) : ElectronicInvoiceAuthorizationRetryRunner {
    override fun retry(command: QuerySriAuthorizationCommand): ElectronicInvoiceAuthorizationRetryOutcome {
        val result = useCase.execute(command)
        return ElectronicInvoiceAuthorizationRetryOutcome(
            record = result.record,
            authorizationStatus = result.authorization.status,
            authorized = result.authorization.isAuthorized,
            artifacts = result.artifacts,
        )
    }
}

interface ElectronicInvoiceDeliveryRunner {
    fun deliver(command: EmailElectronicInvoiceCommand): ElectronicInvoiceDeliveryOutcome
}

class EmailElectronicInvoiceUseCaseDeliveryRunner(
    private val useCase: EmailElectronicInvoiceUseCase,
) : ElectronicInvoiceDeliveryRunner {
    override fun deliver(command: EmailElectronicInvoiceCommand): ElectronicInvoiceDeliveryOutcome {
        val result = useCase.execute(command)
        return ElectronicInvoiceDeliveryOutcome(
            record = result.record,
            delivered = result.delivered,
        )
    }
}
