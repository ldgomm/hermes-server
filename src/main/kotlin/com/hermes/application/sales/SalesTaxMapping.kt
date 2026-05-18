package com.hermes.application.sales

import com.hermes.domain.money.Money
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.sale.SaleItemTax
import com.hermes.domain.sale.TaxProfileSnapshotForSale
import com.hermes.domain.tax.TaxLineResult
import com.hermes.domain.tax.TaxProfileSnapshot
import java.time.ZoneOffset

internal fun TaxLineResult.toSaleItemTax(): SaleItemTax =
    SaleItemTax(
        taxCode = taxProfileSnapshot.sriTaxCode ?: taxProfileSnapshot.taxKind?.name ?: "NO_TAX",
        rateCode = taxProfileSnapshot.sriRateCode ?: taxProfileSnapshot.rateCode ?: taxProfileSnapshot.profileCode,
        rate = Percentage.of(taxProfileSnapshot.rate),
        taxableBase = baseForSri,
        amount = taxAmount,
    )

internal fun TaxProfileSnapshot.toSaleSnapshot(): TaxProfileSnapshotForSale =
    TaxProfileSnapshotForSale(
        code = profileCode,
        taxName = profileName,
        rate = Percentage.of(rate),
        sriTaxCode = sriTaxCode ?: "NO_SRI_TAX_CODE",
        sriRateCode = sriRateCode ?: "NO_SRI_RATE_CODE",
        treatment = treatment,
        legalBasis = legalBasis,
        effectiveFrom = effectiveFrom.atZone(ZoneOffset.UTC).toLocalDate(),
        source = source.name,
    )

internal fun Money.isPositive(): Boolean = amount.signum() > 0
