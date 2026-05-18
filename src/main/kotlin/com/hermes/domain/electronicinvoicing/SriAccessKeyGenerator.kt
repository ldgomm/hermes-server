package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SriAccessKeyGenerationCommand(
    val issuedDate: LocalDate,
    val documentType: SriDocumentType,
    val ruc: String,
    val environment: SriEnvironment,
    val series: SriSeries,
    val sequential: SriSequential,
    val numericCode: SriNumericCode,
    val emissionType: SriEmissionType = SriEmissionType.NORMAL,
) {
    init {
        if (!ruc.trim().matches(Regex("\\d{13}"))) {
            throw DomainRuleViolation("Issuer RUC must contain exactly 13 digits.")
        }
    }
}

object SriAccessKeyGenerator {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyyyy")

    fun generate(command: SriAccessKeyGenerationCommand): SriAccessKey {
        val body = buildString {
            append(command.issuedDate.format(dateFormatter))
            append(command.documentType.code)
            append(command.ruc.trim())
            append(command.environment.code)
            append(command.series.value)
            append(command.sequential.formatted)
            append(command.numericCode.value)
            append(command.emissionType.code)
        }

        if (body.length != SriAccessKey.BODY_LENGTH) {
            throw DomainRuleViolation("SRI access key body must contain ${SriAccessKey.BODY_LENGTH} digits.")
        }

        val checkDigit = Modulo11Calculator.calculateCheckDigit(body)
        return SriAccessKey(body + checkDigit.toString())
    }
}
