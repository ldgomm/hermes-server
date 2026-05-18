package com.hermes.infrastructure.mongo.sales

import com.hermes.domain.reservation.ReservationStatus
import com.hermes.domain.sale.SaleOperationalStatus
import org.bson.types.Decimal128
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MongoSalesMappersTest {
    @Test
    fun `maps sale to document and restores aggregate`() {
        val sale = mongoConfirmedSale()

        val document = MongoSalesMappers.saleToDocument(sale)
        val restored = MongoSalesMappers.saleFromDocument(document)

        assertEquals("sale_1", document.getString("_id"))
        assertEquals("confirmed", document.getString("operationalStatus"))
        assertEquals("standard_sale", document.getString("saleType"))
        assertEquals(SaleOperationalStatus.CONFIRMED, restored.operationalStatus)
        assertEquals(sale.id, restored.id)
        assertEquals(sale.organizationId, restored.organizationId)
        assertEquals(sale.items.single().taxProfileSnapshot.code, restored.items.single().taxProfileSnapshot.code)
        assertEquals(sale.totals.grandTotal, restored.totals.grandTotal)

        val totals = document.get("totals") as org.bson.Document
        val grandTotal = totals.get("grandTotal") as org.bson.Document
        assertTrue(grandTotal["amount"] is Decimal128)
    }

    @Test
    fun `maps reservation to document and restores aggregate`() {
        val reservation = mongoReservation()

        val document = MongoSalesMappers.reservationToDocument(reservation)
        val restored = MongoSalesMappers.reservationFromDocument(document)

        assertEquals("res_1", document.getString("_id"))
        assertEquals("pending", document.getString("status"))
        assertEquals(ReservationStatus.SCHEDULED, restored.status)
        assertEquals(reservation.startAt, restored.startAt)
        assertEquals(reservation.endAt, restored.endAt)
        assertEquals(reservation.partySize, restored.partySize)
        assertEquals(reservation.customerSnapshot.displayName, restored.customerSnapshot.displayName)
    }
}
