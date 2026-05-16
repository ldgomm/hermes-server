package com.hermes.domain.activity

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BusinessActivityRulesTest {
    @Test
    fun `allows active activity with workflow mode`() {
        BusinessActivityRules.assertCanActivate(
            activity(workflowModes = setOf(WorkflowMode.QUICK_SALE)),
        )
    }

    @Test
    fun `rejects activity without workflow modes`() {
        assertFailsWith<DomainRuleViolation> {
            BusinessActivityRules.assertCanActivate(activity(workflowModes = emptySet()))
        }
    }

    @Test
    fun `rejects reservation workflow when reservations are not enabled`() {
        assertFailsWith<DomainRuleViolation> {
            BusinessActivityRules.assertCanActivate(
                activity(workflowModes = setOf(WorkflowMode.RESERVATION), allowsReservations = false),
            )
        }
    }

    @Test
    fun `rejects archiving activity with open operations`() {
        assertFailsWith<DomainRuleViolation> {
            BusinessActivityRules.assertCanArchive(activity(), hasOpenSalesOrReservations = true)
        }
    }

    @Test
    fun `rejects using paused activity for sale`() {
        assertFailsWith<DomainRuleViolation> {
            BusinessActivityRules.assertCanUseForSale(activity(status = BusinessActivityStatus.PAUSED))
        }
    }

    private fun activity(
        status: BusinessActivityStatus = BusinessActivityStatus.ACTIVE,
        workflowModes: Set<WorkflowMode> = setOf(WorkflowMode.QUICK_SALE),
        allowsReservations: Boolean = true,
    ): BusinessActivity = BusinessActivity(
        id = "act_1",
        organizationId = "org_1",
        name = "Restaurante",
        type = BusinessActivityType.RESTAURANT,
        status = status,
        workflowModes = workflowModes,
        allowsReservations = allowsReservations,
    )
}
