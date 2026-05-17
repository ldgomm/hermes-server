package com.hermes.domain.tax

import com.hermes.domain.shared.DomainRuleViolation

object TaxEmissionValidation {
    fun assertCanPrepareEmission(
        emissionType: TaxEmissionType,
        lines: List<TaxLineResult>,
        summary: TaxSummary,
    ) {
        if (lines.isEmpty()) throw DomainRuleViolation("Emission requires at least one tax line.")
        val calculated = TaxSummary.from(lines)
        if (calculated != summary) {
            throw DomainRuleViolation("Tax summary does not match tax lines.")
        }

        if (emissionType == TaxEmissionType.ELECTRONIC_INVOICE) {
            lines.forEach { line ->
                val snapshot = line.taxProfileSnapshot
                if (!snapshot.isElectronicEmissionCompatible) {
                    throw DomainRuleViolation("Line ${line.lineId} cannot be emitted electronically with tax profile ${snapshot.profileCode}.")
                }
            }
        }
    }
}
