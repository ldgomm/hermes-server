package com.hermes.domain.document

import com.hermes.domain.money.CurrencyCode
import com.hermes.domain.money.Money
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.sale.TaxProfileSnapshotForSale
import com.hermes.domain.shared.DomainRuleViolation

data class CommercialDocumentTotalsSnapshot(
    val subtotal: Money,
    val discount: Money,
    val taxTotal: Money,
    val grandTotal: Money,
    val paidAmount: Money,
    val currency: CurrencyCode,
    val paymentStatus: String,
) {
    init {
        listOf(subtotal, discount, taxTotal, grandTotal, paidAmount).forEach { amount ->
            if (amount.currency != currency) throw DomainRuleViolation("Commercial document totals must share one currency.")
        }
        if (paymentStatus.isBlank()) throw DomainRuleViolation("Commercial document payment status cannot be blank.")
    }
}

data class CommercialDocumentTaxLineSnapshot(
    val taxCode: String,
    val rateCode: String,
    val rate: Percentage,
    val taxableBase: Money,
    val amount: Money,
) {
    init {
        if (taxCode.isBlank()) throw DomainRuleViolation("Commercial document tax code cannot be blank.")
        if (rateCode.isBlank()) throw DomainRuleViolation("Commercial document tax rate code cannot be blank.")
        if (taxableBase.currency != amount.currency) throw DomainRuleViolation("Commercial document tax line currency mismatch.")
    }
}

data class CommercialDocumentTaxSnapshot(
    val taxTotal: Money,
    val taxes: List<CommercialDocumentTaxLineSnapshot>,
) {
    init {
        taxes.forEach { tax ->
            if (tax.amount.currency != taxTotal.currency) throw DomainRuleViolation("Commercial document tax snapshot currency mismatch.")
        }
    }
}

data class CommercialDocumentLineSnapshot(
    val saleItemId: String,
    val catalogItemId: String,
    val description: String,
    val quantity: Quantity,
    val unitPrice: Money,
    val discount: Money,
    val netTotal: Money,
    val taxTotal: Money,
    val lineTotal: Money,
    val taxProfileSnapshot: TaxProfileSnapshotForSale,
) {
    init {
        if (saleItemId.isBlank()) throw DomainRuleViolation("Commercial document line sale item id cannot be blank.")
        if (catalogItemId.isBlank()) throw DomainRuleViolation("Commercial document line catalog item id cannot be blank.")
        if (description.isBlank()) throw DomainRuleViolation("Commercial document line description cannot be blank.")
        listOf(unitPrice, discount, netTotal, taxTotal, lineTotal).forEach { money ->
            if (money.currency != unitPrice.currency) throw DomainRuleViolation("Commercial document line currency mismatch.")
        }
    }
}
