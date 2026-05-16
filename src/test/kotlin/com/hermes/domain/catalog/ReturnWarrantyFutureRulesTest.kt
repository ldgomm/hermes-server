package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ReturnWarrantyFutureRulesTest {
    @Test
    fun `allows exchange only return policy`() {
        ReturnPolicy(
            type = ReturnPolicyType.EXCHANGE_ONLY,
            returnWindowDays = 7,
        )
    }

    @Test
    fun `rejects final sale with return window`() {
        assertFailsWith<DomainRuleViolation> {
            ReturnPolicy(
                type = ReturnPolicyType.FINAL_SALE,
                returnWindowDays = 7,
            )
        }
    }

    @Test
    fun `manufacturer warranty requires provider`() {
        assertFailsWith<DomainRuleViolation> {
            WarrantyPolicy(
                type = WarrantyPolicyType.MANUFACTURER,
                durationDays = 365,
                provider = null,
            )
        }
    }

    @Test
    fun `return is not cancellation flow`() {
        assertFailsWith<DomainRuleViolation> {
            ReturnWarrantyFutureRules.assertReturnIsNotCancellation("canceled")
        }
    }

    @Test
    fun `rejects unrestricted refund for perishable item`() {
        assertFailsWith<DomainRuleViolation> {
            ReturnWarrantyFutureRules.assertPolicyCompatibleWithPerishableItem(
                ReturnPolicy(ReturnPolicyType.REFUND_ALLOWED, returnWindowDays = 3),
                isPerishable = true,
            )
        }
    }
}
