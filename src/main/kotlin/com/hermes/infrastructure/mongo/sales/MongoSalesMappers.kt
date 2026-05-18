package com.hermes.infrastructure.mongo.sales

import com.hermes.domain.document.DocumentStatus
import com.hermes.domain.money.CurrencyCode
import com.hermes.domain.money.Money
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.reservation.Reservation
import com.hermes.domain.reservation.ReservationStatus
import com.hermes.domain.sale.*
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoDecimalMapper
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import org.bson.Document
import java.time.LocalDate

object MongoSalesMappers {
    fun saleToDocument(sale: Sale): Document =
        Document(MongoDocumentFields.ID, sale.id)
            .append(MongoDocumentFields.ORGANIZATION_ID, sale.organizationId)
            .append("branchId", sale.branchId)
            .append("activityId", sale.activityId)
            .append("saleNumber", sale.saleNumber ?: sale.id)
            .append("saleType", saleTypeToStorage(sale.saleType))
            .append("workflowMode", sale.workflowMode.name.lowercase())
            .append("operationalStatus", sale.operationalStatus.name.lowercase())
            .append("paymentStatus", sale.paymentStatus.name.lowercase())
            .append("collectionStatus", collectionStatusForSale(sale))
            .append("documentStatus", DocumentStatus.NOT_REQUIRED.name.lowercase())
            .append("customerId", sale.customerId)
            .append("customerSnapshot", customerSnapshotToDocument(sale.customerSnapshot))
            .append("items", sale.items.map(::saleItemToDocument))
            .append("totals", saleTotalsToDocument(sale))
            .append("taxSummary", taxSummaryToDocument(sale))
            .append("paymentRefs", emptyList<String>())
            .append("documentRefs", emptyList<String>())
            .append("reservationRef", null)
            .append("cashSessionId", sale.cashSessionId)
            .append("dueAt", sale.dueAt?.let(MongoInstantMapper::toDate))
            .append("createdAt", MongoInstantMapper.toDate(sale.createdAt))
            .append("createdBy", null)
            .append("updatedAt", MongoInstantMapper.toDate(sale.updatedAt))
            .append("updatedBy", null)
            .append("version", 1)
            .append(MongoDocumentFields.SCHEMA_VERSION, 1)

    fun saleFromDocument(document: Document): Sale =
        Sale.restore(
            id = document.requiredString(MongoDocumentFields.ID),
            organizationId = document.requiredString(MongoDocumentFields.ORGANIZATION_ID),
            branchId = document.requiredString("branchId"),
            activityId = document.requiredString("activityId"),
            saleNumber = document.optionalString("saleNumber"),
            saleType = saleTypeFromStorage(document.requiredString("saleType")),
            workflowMode = enumFromStorage(document.requiredString("workflowMode")),
            customerId = document.optionalString("customerId"),
            customerSnapshot = customerSnapshotFromDocument(document.optionalDocument("customerSnapshot")),
            items = document.documentList("items").map(::saleItemFromDocument),
            operationalStatus = enumFromStorage(document.requiredString("operationalStatus")),
            dueAt = MongoInstantMapper.readOptional(document, "dueAt"),
            cashSessionId = document.optionalString("cashSessionId"),
            createdAt = MongoInstantMapper.readRequired(document, "createdAt"),
            updatedAt = MongoInstantMapper.readRequired(document, "updatedAt"),
        )

    fun reservationToDocument(reservation: Reservation): Document =
        Document(MongoDocumentFields.ID, reservation.id)
            .append(MongoDocumentFields.ORGANIZATION_ID, reservation.organizationId)
            .append("saleId", reservation.saleId)
            .append("branchId", reservation.branchId)
            .append("activityId", reservation.activityId)
            .append("customerId", reservation.customerId)
            .append("customerSnapshot", customerSnapshotToDocument(reservation.customerSnapshot))
            .append("resourceId", reservation.resourceId)
            .append("startAt", MongoInstantMapper.toDate(reservation.startAt))
            .append("endAt", MongoInstantMapper.toDate(reservation.endAt))
            .append("partySize", reservation.partySize)
            .append("status", reservationStatusToStorage(reservation.status))
            .append("notes", reservation.notes)
            .append("createdAt", MongoInstantMapper.toDate(reservation.createdAt))
            .append("createdBy", null)
            .append("updatedAt", MongoInstantMapper.toDate(reservation.updatedAt))
            .append("updatedBy", null)
            .append("version", 1)
            .append(MongoDocumentFields.SCHEMA_VERSION, 1)

    fun reservationFromDocument(document: Document): Reservation =
        Reservation(
            id = document.requiredString(MongoDocumentFields.ID),
            organizationId = document.requiredString(MongoDocumentFields.ORGANIZATION_ID),
            branchId = document.requiredString("branchId"),
            activityId = document.requiredString("activityId"),
            saleId = document.optionalString("saleId"),
            customerId = document.optionalString("customerId"),
            customerSnapshot = customerSnapshotFromDocument(document.optionalDocument("customerSnapshot")),
            resourceId = document.optionalString("resourceId"),
            startAt = MongoInstantMapper.readRequired(document, "startAt"),
            endAt = MongoInstantMapper.readRequired(document, "endAt"),
            partySize = document.getInteger("partySize", 1),
            status = reservationStatusFromStorage(document.requiredString("status")),
            notes = document.optionalString("notes"),
            createdAt = MongoInstantMapper.readRequired(document, "createdAt"),
            updatedAt = MongoInstantMapper.readRequired(document, "updatedAt"),
        )

    private fun saleItemToDocument(item: SaleItem): Document =
        Document("id", item.id)
            .append("catalogItemId", item.catalogItemId)
            .append("name", item.name)
            .append("unitPrice", moneyToDocument(item.unitPrice))
            .append("quantity", quantityToDocument(item.quantity))
            .append("discount", moneyToDocument(item.discount))
            .append("status", item.status.name.lowercase())
            .append("catalogSnapshot", catalogSnapshotToDocument(item.catalogSnapshot))
            .append("taxProfileSnapshot", taxSnapshotToDocument(item.taxProfileSnapshot))
            .append("taxes", item.taxes.map(::saleItemTaxToDocument))
            .append("grossTotal", moneyToDocument(item.grossTotal))
            .append("netTotal", moneyToDocument(item.netTotal))
            .append("taxTotal", moneyToDocument(item.taxTotal))
            .append("lineTotal", moneyToDocument(item.lineTotal))

    private fun saleItemFromDocument(document: Document): SaleItem =
        SaleItem.create(
            id = document.requiredString("id"),
            catalogItemId = document.requiredString("catalogItemId"),
            name = document.requiredString("name"),
            unitPrice = moneyFromDocument(document.requiredDocument("unitPrice")),
            quantity = quantityFromDocument(document.requiredDocument("quantity")),
            discount = moneyFromDocument(document.requiredDocument("discount")),
            catalogSnapshot = catalogSnapshotFromDocument(document.requiredDocument("catalogSnapshot")),
            taxProfileSnapshot = taxSnapshotFromDocument(document.requiredDocument("taxProfileSnapshot")),
            taxes = document.documentList("taxes").map(::saleItemTaxFromDocument),
        ).let { item ->
            when (enumFromStorage<SaleItemStatus>(document.requiredString("status"))) {
                SaleItemStatus.PENDING -> item
                SaleItemStatus.IN_PROGRESS -> item.start()
                SaleItemStatus.READY -> item.markReady()
                SaleItemStatus.DELIVERED -> item.start().deliver()
                SaleItemStatus.CANCELED -> item.cancel()
            }
        }

    private fun customerSnapshotToDocument(snapshot: CustomerSnapshot): Document =
        Document("customerId", snapshot.customerId)
            .append("displayName", snapshot.displayName)
            .append("taxId", snapshot.taxId)
            .append("taxIdType", snapshot.taxIdType)
            .append("email", snapshot.email)

    private fun customerSnapshotFromDocument(document: Document?): CustomerSnapshot {
        if (document == null) return CustomerSnapshot.finalConsumer()
        return CustomerSnapshot(
            customerId = document.optionalString("customerId"),
            displayName = document.optionalString("displayName") ?: "Consumidor final",
            taxId = document.optionalString("taxId"),
            taxIdType = document.optionalString("taxIdType"),
            email = document.optionalString("email"),
        )
    }

    private fun catalogSnapshotToDocument(snapshot: CatalogItemSnapshot): Document =
        Document("catalogItemId", snapshot.catalogItemId)
            .append("sourceTemplateId", snapshot.sourceTemplateId)
            .append("globalCatalogId", snapshot.globalCatalogId)
            .append("productFamilyId", snapshot.productFamilyId)
            .append("name", snapshot.name)
            .append("type", snapshot.type.name)
            .append("taxProfileId", snapshot.taxProfileId)
            .append("unitCode", snapshot.unitCode)

    private fun catalogSnapshotFromDocument(document: Document): CatalogItemSnapshot =
        CatalogItemSnapshot(
            catalogItemId = document.requiredString("catalogItemId"),
            sourceTemplateId = document.optionalString("sourceTemplateId"),
            globalCatalogId = document.requiredString("globalCatalogId"),
            productFamilyId = document.optionalString("productFamilyId"),
            name = document.requiredString("name"),
            type = enumValueOf(document.requiredString("type")),
            taxProfileId = document.requiredString("taxProfileId"),
            unitCode = document.requiredString("unitCode"),
        )

    private fun taxSnapshotToDocument(snapshot: TaxProfileSnapshotForSale): Document =
        Document("code", snapshot.code)
            .append("taxName", snapshot.taxName)
            .append("rate", MongoDecimalMapper.percentageToDecimal128(snapshot.rate.value))
            .append("sriTaxCode", snapshot.sriTaxCode)
            .append("sriRateCode", snapshot.sriRateCode)
            .append("treatment", snapshot.treatment.name)
            .append("legalBasis", snapshot.legalBasis)
            .append("effectiveFrom", snapshot.effectiveFrom.toString())
            .append("source", snapshot.source)

    private fun taxSnapshotFromDocument(document: Document): TaxProfileSnapshotForSale =
        TaxProfileSnapshotForSale(
            code = document.requiredString("code"),
            taxName = document.requiredString("taxName"),
            rate = Percentage.of(MongoDecimalMapper.readRequired(document, "rate")),
            sriTaxCode = document.requiredString("sriTaxCode"),
            sriRateCode = document.requiredString("sriRateCode"),
            treatment = enumValueOf(document.requiredString("treatment")),
            legalBasis = document.requiredString("legalBasis"),
            effectiveFrom = LocalDate.parse(document.requiredString("effectiveFrom")),
            source = document.requiredString("source"),
        )

    private fun saleItemTaxToDocument(tax: SaleItemTax): Document =
        Document("taxCode", tax.taxCode)
            .append("rateCode", tax.rateCode)
            .append("rate", MongoDecimalMapper.percentageToDecimal128(tax.rate.value))
            .append("taxableBase", moneyToDocument(tax.taxableBase))
            .append("amount", moneyToDocument(tax.amount))

    private fun saleItemTaxFromDocument(document: Document): SaleItemTax =
        SaleItemTax(
            taxCode = document.requiredString("taxCode"),
            rateCode = document.requiredString("rateCode"),
            rate = Percentage.of(MongoDecimalMapper.readRequired(document, "rate")),
            taxableBase = moneyFromDocument(document.requiredDocument("taxableBase")),
            amount = moneyFromDocument(document.requiredDocument("amount")),
        )

    private fun saleTotalsToDocument(sale: Sale): Document =
        Document("subtotal", moneyToDocument(sale.totals.subtotal))
            .append("discount", moneyToDocument(sale.totals.discount))
            .append("taxTotal", moneyToDocument(sale.totals.taxTotal))
            .append("grandTotal", moneyToDocument(sale.totals.grandTotal))
            .append("currency", sale.totals.currency.value)

    private fun taxSummaryToDocument(sale: Sale): Document =
        Document("taxTotal", moneyToDocument(sale.totals.taxTotal))
            .append("taxes", sale.activeItems.flatMap { item -> item.taxes }.map(::saleItemTaxToDocument))

    private fun moneyToDocument(money: Money): Document =
        Document("amount", MongoDecimalMapper.moneyToDecimal128(money.amount))
            .append("currency", money.currency.value)

    private fun moneyFromDocument(document: Document): Money =
        Money.of(
            amount = MongoDecimalMapper.readRequired(document, "amount"),
            currency = CurrencyCode(document.requiredString("currency")),
        )

    private fun quantityToDocument(quantity: Quantity): Document =
        Document("value", MongoDecimalMapper.quantityToDecimal128(quantity.value))
            .append("unitCode", quantity.unitCode)
            .append("allowsDecimal", quantity.allowsDecimal)

    private fun quantityFromDocument(document: Document): Quantity =
        Quantity.of(
            value = MongoDecimalMapper.readRequired(document, "value"),
            unitCode = document.requiredString("unitCode"),
            allowsDecimal = document.getBoolean("allowsDecimal", false),
        )

    private fun saleTypeToStorage(type: SaleType): String = when (type) {
        SaleType.SALE -> "standard_sale"
        SaleType.ORDER -> "internal_order"
        SaleType.RESERVATION -> "reservation_sale"
        SaleType.SERVICE_ORDER -> "service_order"
        SaleType.RENTAL -> "rental_sale"
        SaleType.INTERNAL_ADJUSTMENT -> "standard_sale"
    }

    private fun saleTypeFromStorage(raw: String): SaleType = when (raw.trim().lowercase()) {
        "standard_sale" -> SaleType.SALE
        "internal_order" -> SaleType.ORDER
        "reservation_sale" -> SaleType.RESERVATION
        "service_order" -> SaleType.SERVICE_ORDER
        "rental_sale" -> SaleType.RENTAL
        else -> SaleType.SALE
    }

    private fun collectionStatusForSale(sale: Sale): String = when {
        sale.operationalStatus == SaleOperationalStatus.CANCELED -> "not_applicable"
        sale.dueAt != null -> "pending_receivable"
        else -> "not_applicable"
    }

    private fun reservationStatusToStorage(status: ReservationStatus): String = when (status) {
        ReservationStatus.DRAFT -> "draft"
        ReservationStatus.SCHEDULED -> "pending"
        ReservationStatus.CONFIRMED -> "confirmed"
        ReservationStatus.IN_PROGRESS -> "in_progress"
        ReservationStatus.RESCHEDULED -> "pending"
        ReservationStatus.COMPLETED -> "completed"
        ReservationStatus.NO_SHOW -> "no_show"
        ReservationStatus.CANCELED -> "canceled"
    }

    private fun reservationStatusFromStorage(raw: String): ReservationStatus = when (raw.trim().lowercase()) {
        "draft" -> ReservationStatus.DRAFT
        "pending", "scheduled" -> ReservationStatus.SCHEDULED
        "confirmed" -> ReservationStatus.CONFIRMED
        "in_progress" -> ReservationStatus.IN_PROGRESS
        "rescheduled" -> ReservationStatus.RESCHEDULED
        "completed" -> ReservationStatus.COMPLETED
        "no_show" -> ReservationStatus.NO_SHOW
        "canceled", "expired" -> ReservationStatus.CANCELED
        else -> ReservationStatus.SCHEDULED
    }
}

private fun Document.requiredString(field: String): String =
    getString(field)?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Required string field '$field' is missing or blank.")

private fun Document.optionalString(field: String): String? =
    getString(field)?.takeIf { it.isNotBlank() }

private fun Document.optionalDocument(field: String): Document? =
    this[field] as? Document

private fun Document.requiredDocument(field: String): Document =
    optionalDocument(field)
        ?: throw IllegalArgumentException("Required document field '$field' is missing.")

@Suppress("UNCHECKED_CAST")
private fun Document.documentList(field: String): List<Document> =
    (this[field] as? List<*>)?.filterIsInstance<Document>().orEmpty()

private inline fun <reified T : Enum<T>> enumFromStorage(raw: String): T =
    enumValueOf(raw.trim().uppercase())