package com.hermes.infrastructure.mongo.sales

import com.hermes.application.sales.*
import com.hermes.domain.money.CurrencyCode
import com.hermes.domain.money.Money
import com.hermes.domain.payment.SalePaymentStatus
import com.hermes.domain.sale.SaleOperationalStatus
import com.hermes.domain.sale.SaleType
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoDecimalMapper
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import com.hermes.infrastructure.mongo.repository.core.DocumentMongoRepository
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.bson.conversions.Bson
import java.time.Instant
import java.util.*
import java.util.regex.Pattern

class MongoSalesReadRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.SALES),
    SalesReadRepository {

    override fun search(command: SalesSearchCommand): List<SalesListItem> {
        val filters = mutableListOf<Bson>(organizationFilter(command.organizationId))

        command.branchId.normalized()?.let { filters += Filters.eq("branchId", it) }
        command.activityId.normalized()?.let { filters += Filters.eq("activityId", it) }
        command.customerId.normalized()?.let { filters += Filters.eq("customerId", it) }

        if (command.operationalStatuses.isNotEmpty()) {
            filters += Filters.`in`("operationalStatus", command.operationalStatuses.map { it.toStorage() })
        }
        if (command.paymentStatuses.isNotEmpty()) {
            filters += Filters.`in`("paymentStatus", command.paymentStatuses.map { it.toStorage() })
        }
        if (command.saleTypes.isNotEmpty()) {
            filters += Filters.`in`("saleType", command.saleTypes.map { it.toStorage() })
        }

        addDateRange(filters, command.from, command.to)

        command.query.normalized()?.let { q ->
            val pattern = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE)
            filters += Filters.or(
                Filters.eq(MongoDocumentFields.ID, q),
                Filters.eq("saleNumber", q),
                Filters.regex("customerSnapshot.displayName", pattern),
                Filters.regex("items.name", pattern),
            )
        }

        return findMany(
            filter = Filters.and(filters),
            sort = Sorts.descending("createdAt"),
            limit = command.limit,
        ).map(::toListItem)
    }

    override fun findPending(command: PendingSalesCommand): List<SalesListItem> {
        val filters = mutableListOf<Bson>(
            organizationFilter(command.organizationId),
            Filters.`in`(
                "operationalStatus",
                listOf("draft", "pending", "confirmed", "in_progress", "ready", "delivered"),
            ),
        )

        command.branchId.normalized()?.let { filters += Filters.eq("branchId", it) }
        command.activityId.normalized()?.let { filters += Filters.eq("activityId", it) }

        return findMany(
            filter = Filters.and(filters),
            sort = Sorts.ascending("dueAt", "createdAt"),
            limit = command.limit,
        ).map(::toListItem)
    }

    override fun summarizeDay(command: SalesDaySummaryCommand): SalesDaySummaryResult {
        val filters = mutableListOf<Bson>(organizationFilter(command.organizationId))
        command.branchId.normalized()?.let { filters += Filters.eq("branchId", it) }
        command.activityId.normalized()?.let { filters += Filters.eq("activityId", it) }
        addDateRange(filters, command.from, command.to)

        val sales = findMany(
            filter = Filters.and(filters),
            sort = Sorts.ascending("createdAt"),
            limit = 10_000,
        ).map(::toListItem)

        val currency = sales.firstOrNull()?.grandTotal?.currency ?: CurrencyCode("USD")
        val zero = Money.zero(currency)
        fun sum(selector: (SalesListItem) -> Money): Money =
            sales.fold(zero) { current, item -> current + selector(item) }

        val byOperationalStatus = sales.groupingBy { it.operationalStatus }.eachCount()
        val byPaymentStatus = sales.groupingBy { it.paymentStatus }.eachCount()

        return SalesDaySummaryResult(
            organizationId = command.organizationId,
            branchId = command.branchId,
            activityId = command.activityId,
            from = command.from,
            to = command.to,
            currency = currency.value,
            totalSalesCount = sales.size,
            closedSalesCount = sales.count { it.operationalStatus == SaleOperationalStatus.CLOSED },
            canceledSalesCount = sales.count { it.operationalStatus == SaleOperationalStatus.CANCELED },
            openSalesCount = sales.count { it.isPendingOperationally },
            grossTotal = sum { it.grandTotal },
            discountTotal = zero,
            taxTotal = zero,
            grandTotal = sum { it.grandTotal },
            paidTotal = sum { it.paidAmount },
            receivableTotal = sum { it.receivableAmount },
            byOperationalStatus = byOperationalStatus,
            byPaymentStatus = byPaymentStatus,
        )
    }

    private fun addDateRange(filters: MutableList<Bson>, from: Instant?, to: Instant?) {
        if (from != null && to != null) {
            filters += Filters.and(
                Filters.gte("createdAt", Date.from(from)),
                Filters.lt("createdAt", Date.from(to)),
            )
        } else if (from != null) {
            filters += Filters.gte("createdAt", Date.from(from))
        } else if (to != null) {
            filters += Filters.lt("createdAt", Date.from(to))
        }
    }

    private fun toListItem(document: Document): SalesListItem {
        val totals = document.optionalDocument("totals")
        val customerSnapshot = document.optionalDocument("customerSnapshot")

        val grandTotal = totals.moneyField("grandTotal")
        val paidAmount = document.moneyAtPath("paidAmount") ?: Money.zero(grandTotal.currency)
        val receivableAmount = (grandTotal - paidAmount).coerceNonNegative()

        return SalesListItem(
            id = document.requiredString(MongoDocumentFields.ID),
            organizationId = document.requiredString(MongoDocumentFields.ORGANIZATION_ID),
            branchId = document.requiredString("branchId"),
            activityId = document.requiredString("activityId"),
            saleNumber = document.optionalString("saleNumber"),
            saleType = saleTypeFromStorage(document.optionalString("saleType") ?: "standard_sale"),
            customerId = document.optionalString("customerId"),
            customerDisplayName = customerSnapshot?.optionalString("displayName"),
            operationalStatus = operationalStatusFromStorage(document.requiredString("operationalStatus")),
            paymentStatus = paymentStatusFromStorage(document.optionalString("paymentStatus") ?: "unpaid"),
            itemCount = document.documentList("items").size,
            grandTotal = grandTotal,
            paidAmount = paidAmount,
            receivableAmount = receivableAmount,
            dueAt = MongoInstantMapper.readOptional(document, "dueAt"),
            cashSessionId = document.optionalString("cashSessionId"),
            createdAt = MongoInstantMapper.readRequired(document, "createdAt"),
            updatedAt = MongoInstantMapper.readRequired(document, "updatedAt"),
        )
    }

    private fun Document.moneyAtPath(field: String): Money? =
        optionalDocument(field)?.let(::moneyFromDocument)

    private fun Document?.moneyField(field: String): Money {
        val doc = this?.optionalDocument(field)
            ?: return Money.zero(CurrencyCode("USD"))
        return moneyFromDocument(doc)
    }

    private fun moneyFromDocument(document: Document): Money =
        Money.of(
            amount = MongoDecimalMapper.readRequired(document, "amount"),
            currency = CurrencyCode(document.requiredString("currency")),
        )

    private fun Money.coerceNonNegative(): Money =
        if (amount.signum() < 0) Money.zero(currency) else this

    private fun String?.normalized(): String? =
        this?.trim()?.takeIf { it.isNotBlank() }

    private fun SaleOperationalStatus.toStorage(): String = name.lowercase()

    private fun SalePaymentStatus.toStorage(): String = name.lowercase()

    private fun SaleType.toStorage(): String = when (this) {
        SaleType.SALE -> "standard_sale"
        SaleType.ORDER -> "internal_order"
        SaleType.RESERVATION -> "reservation_sale"
        SaleType.SERVICE_ORDER -> "service_order"
        SaleType.RENTAL -> "rental_sale"
        SaleType.INTERNAL_ADJUSTMENT -> "internal_adjustment"
    }

    private fun saleTypeFromStorage(raw: String): SaleType = when (raw.trim().lowercase()) {
        "standard_sale", "sale" -> SaleType.SALE
        "internal_order", "order" -> SaleType.ORDER
        "reservation_sale", "reservation" -> SaleType.RESERVATION
        "service_order" -> SaleType.SERVICE_ORDER
        "rental_sale", "rental" -> SaleType.RENTAL
        "internal_adjustment" -> SaleType.INTERNAL_ADJUSTMENT
        else -> SaleType.SALE
    }

    private fun operationalStatusFromStorage(raw: String): SaleOperationalStatus =
        enumValueOf(raw.trim().uppercase())

    private fun paymentStatusFromStorage(raw: String): SalePaymentStatus =
        enumValueOf(raw.trim().uppercase())
}

private fun Document.requiredString(field: String): String =
    getString(field)?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Required string field '$field' is missing or blank.")

private fun Document.optionalString(field: String): String? =
    getString(field)?.takeIf { it.isNotBlank() }

private fun Document.optionalDocument(field: String): Document? =
    this[field] as? Document

@Suppress("UNCHECKED_CAST")
private fun Document.documentList(field: String): List<Document> =
    (this[field] as? List<*>)?.filterIsInstance<Document>().orEmpty()
