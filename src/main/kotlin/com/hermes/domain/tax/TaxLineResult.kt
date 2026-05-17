package com.hermes.domain.tax

import com.hermes.domain.money.Money

data class TaxLineResult(
    val lineId: String,
    val description: String,
    val grossAmount: Money,
    val discount: Money,
    val taxableBase: Money,
    val zeroRateBase: Money,
    val exemptBase: Money,
    val notSubjectBase: Money,
    val internalNoTaxBase: Money,
    val taxAmount: Money,
    val total: Money,
    val taxProfileSnapshot: TaxProfileSnapshot,
) {
    val baseForSri: Money
        get() = when (taxProfileSnapshot.treatment) {
            TaxTreatment.IVA_FULL,
            TaxTreatment.IVA_REDUCED_OR_SPECIAL -> taxableBase
            TaxTreatment.IVA_ZERO -> zeroRateBase
            TaxTreatment.EXEMPT_IVA -> exemptBase
            TaxTreatment.NOT_SUBJECT_TO_IVA -> notSubjectBase
            TaxTreatment.NO_TAX_INTERNAL -> internalNoTaxBase
        }
}
