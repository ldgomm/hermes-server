package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicSequence
import com.hermes.domain.electronicinvoicing.SriAccessKeyGenerationCommand
import com.hermes.domain.electronicinvoicing.SriAccessKeyGenerator
import com.hermes.domain.electronicinvoicing.SriEmissionType

class EnsureElectronicSequenceUseCase(
    private val repository: ElectronicSequenceRepository,
) {
    fun execute(command: EnsureElectronicSequenceCommand): ElectronicSequenceResult {
        val sequence = ElectronicSequence.create(
            id = command.id,
            organizationId = command.organizationId,
            environment = command.environment,
            documentType = command.documentType,
            series = command.series,
            startsAfter = command.startsAfter,
            now = command.now,
        )
        return ElectronicSequenceResult(repository.createIfMissing(sequence))
    }
}

class ReserveSriAccessKeyUseCase(
    private val repository: ElectronicSequenceRepository,
) {
    fun execute(command: ReserveSriAccessKeyCommand): ReserveSriAccessKeyResult {
        command.documentType.assertMvpSupported()

        val reservation = repository.nextSequential(
            NextElectronicSequentialCommand(
                organizationId = command.organizationId,
                environment = command.environment,
                documentType = command.documentType,
                series = command.series,
                documentId = command.documentId,
                issuedAt = command.issuedAt,
            )
        )

        val accessKey = SriAccessKeyGenerator.generate(
            SriAccessKeyGenerationCommand(
                issuedDate = command.issuedDate,
                documentType = command.documentType,
                ruc = command.ruc,
                environment = command.environment,
                series = command.series,
                sequential = reservation.sequential,
                numericCode = command.numericCode,
                emissionType = SriEmissionType.NORMAL,
            )
        )

        return ReserveSriAccessKeyResult(
            sequence = reservation.sequence,
            sequential = reservation.sequential,
            documentNumber = reservation.documentNumber,
            accessKey = accessKey,
            authorizationNumber = accessKey.value,
        )
    }
}
