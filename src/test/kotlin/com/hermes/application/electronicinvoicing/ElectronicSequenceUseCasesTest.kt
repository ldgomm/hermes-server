package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicSequence
import com.hermes.domain.electronicinvoicing.ElectronicSequenceKey
import com.hermes.domain.electronicinvoicing.ElectronicSequenceReservation
import com.hermes.domain.electronicinvoicing.SriDocumentType
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriNumericCode
import com.hermes.domain.electronicinvoicing.SriSeries
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ElectronicSequenceUseCasesTest {
    private val now = Instant.parse("2026-05-18T10:00:00Z")

    @Test
    fun `ensure sequence is idempotent by logical key`() {
        val repository = InMemoryElectronicSequenceRepository()
        val useCase = EnsureElectronicSequenceUseCase(repository)

        val command = EnsureElectronicSequenceCommand(
            id = "eseq_001",
            organizationId = "org_test",
            environment = SriEnvironment.TEST,
            documentType = SriDocumentType.INVOICE,
            series = SriSeries("001", "002"),
            startsAfter = 41,
            now = now,
        )

        val first = useCase.execute(command).sequence
        val second = useCase.execute(command.copy(id = "eseq_duplicate", startsAfter = 100)).sequence

        assertEquals(first.id, second.id)
        assertEquals(41, second.currentValue)
    }

    @Test
    fun `reserve access key advances sequential and builds document number`() {
        val repository = InMemoryElectronicSequenceRepository()
        EnsureElectronicSequenceUseCase(repository).execute(
            EnsureElectronicSequenceCommand(
                id = "eseq_001",
                organizationId = "org_test",
                environment = SriEnvironment.TEST,
                documentType = SriDocumentType.INVOICE,
                series = SriSeries("001", "002"),
                now = now,
            )
        )

        val result = ReserveSriAccessKeyUseCase(repository).execute(
            ReserveSriAccessKeyCommand(
                organizationId = "org_test",
                environment = SriEnvironment.TEST,
                ruc = "1790012345001",
                series = SriSeries("001", "002"),
                issuedDate = LocalDate.of(2026, 5, 18),
                numericCode = SriNumericCode("12345678"),
                documentId = "doc_001",
                issuedAt = now.plusSeconds(1),
            )
        )

        assertEquals("000000001", result.sequential.formatted)
        assertEquals("001-002-000000001", result.documentNumber)
        assertEquals(result.accessKey.value, result.authorizationNumber)
        assertEquals(49, result.accessKey.value.length)
        assertTrue(result.accessKey.value.startsWith("180520260117900123450011001002000000001123456781"))
    }

    @Test
    fun `reserve access key keeps independent sequences by environment`() {
        val repository = InMemoryElectronicSequenceRepository()
        val ensure = EnsureElectronicSequenceUseCase(repository)
        val series = SriSeries("001", "002")

        ensure.execute(
            EnsureElectronicSequenceCommand(
                id = "eseq_test",
                organizationId = "org_test",
                environment = SriEnvironment.TEST,
                documentType = SriDocumentType.INVOICE,
                series = series,
                now = now,
            )
        )
        ensure.execute(
            EnsureElectronicSequenceCommand(
                id = "eseq_prod",
                organizationId = "org_test",
                environment = SriEnvironment.PRODUCTION,
                documentType = SriDocumentType.INVOICE,
                series = series,
                now = now,
            )
        )

        val reserve = ReserveSriAccessKeyUseCase(repository)
        val test = reserve.execute(
            ReserveSriAccessKeyCommand(
                organizationId = "org_test",
                environment = SriEnvironment.TEST,
                ruc = "1790012345001",
                series = series,
                issuedDate = LocalDate.of(2026, 5, 18),
                numericCode = SriNumericCode("11111111"),
                issuedAt = now.plusSeconds(1),
            )
        )
        val prod = reserve.execute(
            ReserveSriAccessKeyCommand(
                organizationId = "org_test",
                environment = SriEnvironment.PRODUCTION,
                ruc = "1790012345001",
                series = series,
                issuedDate = LocalDate.of(2026, 5, 18),
                numericCode = SriNumericCode("22222222"),
                issuedAt = now.plusSeconds(2),
            )
        )

        assertEquals("000000001", test.sequential.formatted)
        assertEquals("000000001", prod.sequential.formatted)
        assertEquals(SriEnvironment.TEST, test.accessKey.environment)
        assertEquals(SriEnvironment.PRODUCTION, prod.accessKey.environment)
    }
}

private class InMemoryElectronicSequenceRepository : ElectronicSequenceRepository {
    private val sequences = linkedMapOf<ElectronicSequenceKey, ElectronicSequence>()

    override fun createIfMissing(sequence: ElectronicSequence): ElectronicSequence = synchronized(this) {
        sequences.getOrPut(sequence.key) { sequence }
    }

    override fun findByKey(key: ElectronicSequenceKey): ElectronicSequence? = synchronized(this) {
        sequences[key]
    }

    override fun nextSequential(command: NextElectronicSequentialCommand): ElectronicSequenceReservation = synchronized(this) {
        val key = ElectronicSequenceKey(
            organizationId = command.organizationId,
            environment = command.environment,
            documentType = command.documentType,
            series = command.series,
        )
        val current = sequences[key] ?: error("Sequence does not exist: ${key.storageKey}")
        val next = current.nextSequential()
        val updated = current.markIssued(next, command.documentId, command.issuedAt)
        sequences[key] = updated
        ElectronicSequenceReservation(sequence = updated, sequential = next)
    }
}
