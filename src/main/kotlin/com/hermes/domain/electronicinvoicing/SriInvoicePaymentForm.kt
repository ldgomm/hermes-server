package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation

/**
 * SRI XML accepts payment form codes from 01 to 21 in the invoice XSD family.
 * This value object deliberately stores the raw SRI code because the catalog can
 * change and the business payment method mapping must remain configurable.
 */
@JvmInline
value class SriInvoicePaymentForm(val code: String) {
    init {
        if (code != code.trim() || !Regex("^(0[1-9]|1[0-9]|2[0-1])$").matches(code)) {
            throw DomainRuleViolation("SRI payment form code must be normalized and between 01 and 21.")
        }
    }

    companion object {
        val WITHOUT_FINANCIAL_SYSTEM = SriInvoicePaymentForm("01")
        val OTHER_WITH_FINANCIAL_SYSTEM = SriInvoicePaymentForm("20")
        val CREDIT_CARD = SriInvoicePaymentForm("19")
    }
}
