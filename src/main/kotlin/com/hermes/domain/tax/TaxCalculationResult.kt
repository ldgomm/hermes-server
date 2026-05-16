package com.hermes.domain.tax

data class TaxCalculationResult(
    val lines: List<TaxLineResult>,
    val summary: TaxSummary,
)
