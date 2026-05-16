package com.hermes.infrastructure.mongo.mapping

import kotlin.test.Test
import kotlin.test.assertTrue

class EnumPersistenceMappingTest {
    @Test
    fun `all registered enum mappings are complete and unique`() {
        EnumPersistenceMappings.all.forEach { mapping ->
            assertTrue(mapping.enumToValue.isNotEmpty(), "Mapping ${mapping.enumType} must not be empty")
            val duplicatedValues = mapping.enumToValue.values
                .groupBy { it }
                .filterValues { it.size > 1 }

            assertTrue(
                duplicatedValues.isEmpty(),
                "Mapping ${mapping.enumType} has duplicated persistence values: $duplicatedValues",
            )
        }
    }

    @Test
    fun `payment lifecycle and sale payment status are deliberately separate`() {
        val saleValues = EnumPersistenceMappings.salePaymentStatus.enumToValue.values.toSet()
        val paymentValues = EnumPersistenceMappings.paymentLifecycleStatus.enumToValue.values.toSet()

        assertTrue("unpaid" in saleValues)
        assertTrue("partially_paid" in saleValues)
        assertTrue("allocated" in paymentValues)
        assertTrue("confirmed" in paymentValues)
        assertTrue("allocated" !in saleValues)
    }

    @Test
    fun `activity workflow and sale workflow are deliberately separate`() {
        val activityValues = EnumPersistenceMappings.activityWorkflowMode.enumToValue.values.toSet()
        val saleValues = EnumPersistenceMappings.saleWorkflowMode.enumToValue.values.toSet()

        assertTrue("order" in activityValues)
        assertTrue("table_order" in saleValues)
        assertTrue("delivery_order" in saleValues)
        assertTrue("table_order" !in activityValues)
    }
}
