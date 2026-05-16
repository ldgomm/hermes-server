package com.hermes.infrastructure.mongo.mapping

import com.hermes.domain.activity.ActivityWorkflowMode
import com.hermes.domain.catalog.CatalogItemStatus
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.catalog.CatalogTemplateStatus
import com.hermes.domain.document.DocumentStatus
import com.hermes.domain.payment.PaymentLifecycleStatus
import com.hermes.domain.payment.ReceivableStatus
import com.hermes.domain.payment.SalePaymentStatus
import com.hermes.domain.sale.SaleOperationalStatus
import com.hermes.domain.sale.SaleType
import com.hermes.domain.sale.SaleWorkflowMode

object EnumPersistenceMappings {
    val saleOperationalStatus = enumMap<SaleOperationalStatus>(
        "DRAFT" to "draft",
        "PENDING" to "pending",
        "CONFIRMED" to "confirmed",
        "IN_PROGRESS" to "in_progress",
        "READY" to "ready",
        "DELIVERED" to "delivered",
        "CLOSED" to "closed",
        "CANCELED" to "canceled",
    )

    val salePaymentStatus = enumMap<SalePaymentStatus>(
        "UNPAID" to "unpaid",
        "PARTIALLY_PAID" to "partially_paid",
        "PAID" to "paid",
        "OVERPAID" to "overpaid",
        "REFUNDED" to "refunded",
        "VOIDED" to "voided",
    )

    val paymentLifecycleStatus = enumMap<PaymentLifecycleStatus>(
        "DRAFT" to "draft",
        "PENDING" to "pending",
        "CONFIRMED" to "confirmed",
        "ALLOCATED" to "allocated",
        "REVERSED" to "reversed",
        "VOIDED" to "voided",
        "FAILED" to "failed",
    )

    val receivableStatus = enumMap<ReceivableStatus>(
        "NOT_APPLICABLE" to "not_applicable",
        "PENDING_RECEIVABLE" to "pending_receivable",
        "PARTIALLY_COLLECTED" to "partially_collected",
        "SETTLED" to "settled",
        "OVERDUE" to "overdue",
        "WRITTEN_OFF" to "written_off",
        "CANCELED" to "canceled",
    )

    val activityWorkflowMode = enumMap<ActivityWorkflowMode>(
        "QUICK_SALE" to "quick_sale",
        "ORDER" to "order",
        "RESERVATION" to "reservation",
        "SERVICE_ORDER" to "service_order",
        "RENTAL" to "rental",
    )

    val saleWorkflowMode = enumMap<SaleWorkflowMode>(
        "QUICK_SALE" to "quick_sale",
        "COUNTER_ORDER" to "counter_order",
        "TABLE_ORDER" to "table_order",
        "DELIVERY_ORDER" to "delivery_order",
        "RESERVATION" to "reservation",
        "APPOINTMENT" to "appointment",
        "SERVICE_ORDER" to "service_order",
        "RENTAL" to "rental",
        "QUOTE_TO_SALE" to "quote_to_sale",
    )

    val saleType = enumMap<SaleType>(
        "SALE" to "sale",
        "ORDER" to "order",
        "RESERVATION" to "reservation",
        "SERVICE_ORDER" to "service_order",
        "RENTAL" to "rental",
        "INTERNAL_ADJUSTMENT" to "internal_adjustment",
    )

    val documentStatus = enumMap<DocumentStatus>(
        "NOT_REQUIRED" to "not_required",
        "DRAFT" to "draft",
        "GENERATED" to "generated",
        "VALIDATED" to "validated",
        "SIGNED" to "signed",
        "SENT" to "sent",
        "RECEIVED" to "received",
        "AUTHORIZED" to "authorized",
        "REJECTED" to "rejected",
        "RETURNED" to "returned",
        "CANCELLATION_REQUESTED" to "cancellation_requested",
        "PENDING_CANCELLATION" to "pending_cancellation",
        "CANCELED" to "canceled",
        "ERROR" to "error",
    )

    val catalogTemplateStatus = enumMap<CatalogTemplateStatus>(
        "DRAFT" to "draft",
        "ACTIVE" to "published",
        "PAUSED" to "deprecated",
        "ARCHIVED" to "archived",
    )

    val catalogItemStatus = enumMap<CatalogItemStatus>(
        "DRAFT" to "draft",
        "ACTIVE" to "active",
        "PAUSED" to "inactive",
        "OUT_OF_STOCK" to "out_of_stock",
        "ARCHIVED" to "archived",
        "REMOVED_FROM_ACCOUNT" to "removed_from_account",
    )

    val catalogItemType = enumMap<CatalogItemType>(
        "PRODUCT" to "product",
        "SERVICE" to "service",
        "PACKAGE" to "package",
        "RENTAL" to "rental",
        "FEE" to "fee",
    )

    val all: List<EnumPersistenceMapping<out Enum<*>>> = listOf(
        saleOperationalStatus,
        salePaymentStatus,
        paymentLifecycleStatus,
        receivableStatus,
        activityWorkflowMode,
        saleWorkflowMode,
        saleType,
        documentStatus,
        catalogTemplateStatus,
        catalogItemStatus,
        catalogItemType,
    )

    inline fun <reified E : Enum<E>> enumMap(vararg pairs: Pair<String, String>): EnumPersistenceMapping<E> {
        val byEnumName = pairs.toMap()
        val entriesByName = enumValues<E>().associateBy { it.name }
        val enumToValue = entriesByName.mapValues { (name, _) ->
            byEnumName[name] ?: error("Missing persistence mapping for ${E::class.simpleName}.$name")
        }
        return EnumPersistenceMapping(
            enumType = E::class.java.simpleName,
            enumToValue = enumToValue,
        )
    }
}

data class EnumPersistenceMapping<E : Enum<E>>(
    val enumType: String,
    val enumToValue: Map<String, String>,
) {
    fun valueOf(enumName: String): String =
        enumToValue[enumName] ?: error("Missing persistence mapping for $enumType.$enumName")

    fun assertComplete(expectedNames: Set<String>) {
        val missing = expectedNames - enumToValue.keys
        if (missing.isNotEmpty()) error("Missing $enumType mappings: $missing")

        val duplicatedValues = enumToValue.values
            .groupBy { it }
            .filterValues { it.size > 1 }
            .keys

        if (duplicatedValues.isNotEmpty()) {
            error("Duplicated persistence values in $enumType: $duplicatedValues")
        }
    }
}
