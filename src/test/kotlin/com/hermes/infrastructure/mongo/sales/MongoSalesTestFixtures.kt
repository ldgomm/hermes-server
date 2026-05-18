package com.hermes.infrastructure.mongo.sales

import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.money.Money
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.reservation.Reservation
import com.hermes.domain.sale.*
import com.hermes.domain.tax.TaxTreatment
import java.time.Instant
import java.time.LocalDate

internal val MongoSalesTestNow: Instant = Instant.parse("2026-05-18T10:00:00Z")

internal fun mongoSaleItem(
    id: String = "sitem_1",
    catalogItemId: String = "cat_1",
    name: String = "Café americano",
    unitPrice: Money = Money.of("10.00"),
    quantity: Quantity = Quantity.units(2),
): SaleItem = SaleItem.create(
    id = id,
    catalogItemId = catalogItemId,
    name = name,
    unitPrice = unitPrice,
    quantity = quantity,
    discount = Money.zero(unitPrice.currency),
    catalogSnapshot = CatalogItemSnapshot(
        catalogItemId = catalogItemId,
        sourceTemplateId = "tpl_$catalogItemId",
        globalCatalogId = "global_$catalogItemId",
        productFamilyId = null,
        name = name,
        type = CatalogItemType.PRODUCT,
        taxProfileId = "taxp_iva13",
        unitCode = "unit",
    ),
    taxProfileSnapshot = TaxProfileSnapshotForSale(
        code = "iva_current_full",
        taxName = "IVA current full",
        rate = Percentage.of("13.0000".toBigDecimal()),
        sriTaxCode = "2",
        sriRateCode = "4",
        treatment = TaxTreatment.IVA_FULL,
        legalBasis = "Test legal basis",
        effectiveFrom = LocalDate.parse("2026-01-01"),
        source = "SYSTEM_SEED",
    ),
    taxes = listOf(
        SaleItemTax(
            taxCode = "2",
            rateCode = "4",
            rate = Percentage.of("13.0000".toBigDecimal()),
            taxableBase = Money.of("20.00"),
            amount = Money.of("2.60"),
        )
    ),
)

internal fun mongoDraftSale(
    id: String = "sale_1",
    customerId: String? = "cust_1",
    activityId: String = "act_restaurant",
    createdAt: Instant = MongoSalesTestNow,
): Sale = Sale.createDraft(
    id = id,
    organizationId = "org_1",
    branchId = "br_1",
    activityId = activityId,
    saleType = SaleType.SALE,
    workflowMode = SaleWorkflowMode.QUICK_SALE,
    saleNumber = "SALE-001",
    customerId = customerId,
    customerSnapshot = CustomerSnapshot(
        customerId = customerId,
        displayName = "Ana Cliente",
        taxId = "1720000001",
        taxIdType = "cedula",
        email = "ana@example.com",
    ),
    cashSessionId = "cash_1",
    createdAt = createdAt,
).addItem(mongoSaleItem(), createdAt)

internal fun mongoConfirmedSale(
    id: String = "sale_1",
    customerId: String? = "cust_1",
    activityId: String = "act_restaurant",
    createdAt: Instant = MongoSalesTestNow,
): Sale = mongoDraftSale(
    id = id,
    customerId = customerId,
    activityId = activityId,
    createdAt = createdAt,
).confirm(createdAt)

internal fun mongoReservation(
    id: String = "res_1",
    activityId: String = "act_tourism",
    customerId: String? = "cust_1",
): Reservation = Reservation.schedule(
    id = id,
    organizationId = "org_1",
    branchId = "br_1",
    activityId = activityId,
    saleId = "sale_1",
    customerId = customerId,
    customerSnapshot = CustomerSnapshot(
        customerId = customerId,
        displayName = "Ana Cliente",
        taxId = "1720000001",
        taxIdType = "cedula",
        email = "ana@example.com",
    ),
    resourceId = "quad_1",
    startAt = MongoSalesTestNow.plusSeconds(3600),
    endAt = MongoSalesTestNow.plusSeconds(7200),
    partySize = 2,
    notes = "Reserva de turismo",
    createdAt = MongoSalesTestNow,
)
