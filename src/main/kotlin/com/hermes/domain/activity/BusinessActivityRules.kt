package com.hermes.domain.activity

import com.hermes.domain.shared.DomainRuleViolation

object BusinessActivityRules {
    fun assertCanActivate(activity: BusinessActivity) {
        if (activity.workflowModes.isEmpty()) {
            throw DomainRuleViolation("Active business activity requires at least one workflow mode.")
        }
        if (WorkflowMode.RESERVATION in activity.workflowModes && !activity.allowsReservations) {
            throw DomainRuleViolation("Reservation workflow requires allowsReservations enabled.")
        }
        if (activity.status == BusinessActivityStatus.ARCHIVED) {
            throw DomainRuleViolation("Archived business activity cannot be activated.")
        }
    }

    fun assertCanArchive(activity: BusinessActivity, hasOpenSalesOrReservations: Boolean) {
        if (hasOpenSalesOrReservations) {
            throw DomainRuleViolation("Business activity cannot be archived while it has open operations.")
        }
        if (activity.status == BusinessActivityStatus.ARCHIVED) {
            throw DomainRuleViolation("Business activity is already archived.")
        }
    }

    fun assertCanUseForSale(activity: BusinessActivity) {
        if (activity.status != BusinessActivityStatus.ACTIVE) {
            throw DomainRuleViolation("Only an active business activity can be used for a sale.")
        }
    }
}
