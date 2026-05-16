package com.hermes.domain.tax

import com.hermes.domain.money.Money

data class TaxLineResult(
    val lineId: String,
    val description: String,
    val snapshot: TaxProfileSnapshot,
    val grossAmount: Money,
    val discount: Money,
    val taxableBase: Money,
    val taxAmount: Money,
    val total: Money,
)
