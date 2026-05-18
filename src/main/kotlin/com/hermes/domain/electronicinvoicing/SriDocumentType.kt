package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation

enum class SriDocumentType(
    val code: String,
    val storageValue: String,
    val displayName: String,
) {
    INVOICE("01", "electronic_invoice", "Factura"),
    PURCHASE_LIQUIDATION("03", "purchase_liquidation", "Liquidación de compra"),
    CREDIT_NOTE("04", "credit_note", "Nota de crédito"),
    DEBIT_NOTE("05", "debit_note", "Nota de débito"),
    REMISSION_GUIDE("06", "remission_guide", "Guía de remisión"),
    WITHHOLDING("07", "withholding", "Comprobante de retención");

    val isMvpSupported: Boolean get() = this == INVOICE
    val supportsFinalConsumer: Boolean get() = this == INVOICE

    fun assertMvpSupported() {
        if (!isMvpSupported) {
            throw DomainRuleViolation("SRI document type $code is not supported in the electronic invoice MVP.")
        }
    }

    companion object {
        fun fromCode(code: String): SriDocumentType {
            val normalized = code.trim()
            return entries.firstOrNull { it.code == normalized }
                ?: throw DomainRuleViolation("Unknown SRI document type code: $code.")
        }

        fun fromStorage(value: String): SriDocumentType {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { it.storageValue == normalized }
                ?: throw DomainRuleViolation("Unknown SRI document type: $value.")
        }
    }
}
