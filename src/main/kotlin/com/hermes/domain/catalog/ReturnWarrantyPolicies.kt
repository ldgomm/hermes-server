package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation

data class ReturnPolicy(
    val type: ReturnPolicyType,
    val returnWindowDays: Int,
    val requiresOriginalReceipt: Boolean = true,
    val restockingAllowed: Boolean = false,
) {
    init {
        if (returnWindowDays < 0) throw DomainRuleViolation("Return window cannot be negative.")
        if (type == ReturnPolicyType.FINAL_SALE && returnWindowDays != 0) {
            throw DomainRuleViolation("Final sale return policy must have zero return window days.")
        }
    }
}

data class WarrantyPolicy(
    val type: WarrantyPolicyType,
    val durationDays: Int,
    val provider: String? = null,
) {
    init {
        if (durationDays < 0) throw DomainRuleViolation("Warranty duration cannot be negative.")
        if (type == WarrantyPolicyType.NONE && durationDays != 0) {
            throw DomainRuleViolation("No-warranty policy must have zero duration days.")
        }
        if (type == WarrantyPolicyType.MANUFACTURER && provider.isNullOrBlank()) {
            throw DomainRuleViolation("Manufacturer warranty requires provider.")
        }
    }
}

object ReturnWarrantyFutureRules {
    fun assertReturnIsNotCancellation(saleStatus: String) {
        if (saleStatus.lowercase() == "canceled") {
            throw DomainRuleViolation("Returns and warranties are not cancellation flows.")
        }
    }

    fun assertPolicyCompatibleWithPerishableItem(policy: ReturnPolicy, isPerishable: Boolean) {
        if (isPerishable && policy.type == ReturnPolicyType.REFUND_ALLOWED) {
            throw DomainRuleViolation("Perishable items cannot use unrestricted refund policy.")
        }
    }
}
