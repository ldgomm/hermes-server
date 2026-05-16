package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.migration.HermesMongoMigrations
import com.hermes.infrastructure.mongo.migration.MongoMigrationRunner
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.bson.types.Decimal128
import java.math.BigDecimal
import java.time.Instant
import java.util.Date

internal object RepositoryTestSupport {
    const val ORGANIZATION_ID = "org_phase43"
    const val OTHER_ORGANIZATION_ID = "org_phase43_other"
    const val BRANCH_ID = "br_phase43_main"
    const val OTHER_BRANCH_ID = "br_phase43_secondary"
    const val ACTIVITY_ID = "act_phase43_restaurant"
    const val CUSTOMER_ID = "cus_phase43_customer"
    const val USER_ID = "usr_phase43_operator"
    const val CASH_SESSION_ID = "cash_phase43_open"
    const val SALE_ID = "sale_phase43_001"
    const val CATALOG_ITEM_ID = "item_phase43_cuy"
    const val DOCUMENT_ID = "doc_phase43_001"
    const val PAYLOAD_ID = "epay_phase43_001"
    const val ACCESS_KEY = "1234567890123456789012345678901234567890123456789"

    fun withMigratedDatabase(
        prefix: String,
        block: (MongoDatabase) -> Unit,
    ) {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database = client.getDatabase(MongoIntegrationTestSupport.databaseName(prefix))
            MongoMigrationRunner(database).migrate(HermesMongoMigrations.all)
            block(database)
        }
    }

    fun root(
        id: String,
        organizationId: String? = ORGANIZATION_ID,
        instant: Instant = Instant.parse("2026-05-15T12:00:00Z"),
    ): Document {
        val document = Document("_id", id)
            .append("createdAt", Date.from(instant))
            .append("createdBy", USER_ID)
            .append("updatedAt", Date.from(instant))
            .append("updatedBy", USER_ID)
            .append("version", 1)
            .append("schemaVersion", 1)

        if (organizationId != null) {
            document.append("organizationId", organizationId)
        }

        return document
    }

    fun money(amount: String): Document =
        Document("amount", Decimal128(BigDecimal(amount)))
            .append("currency", "USD")

    fun decimal(amount: String): Decimal128 = Decimal128(BigDecimal(amount))

    fun saleDocument(
        id: String = SALE_ID,
        organizationId: String = ORGANIZATION_ID,
        saleNumber: String = "S-000001",
        customerId: String? = CUSTOMER_ID,
        operationalStatus: String = "pending",
        paymentStatus: String = "unpaid",
        collectionStatus: String = "pending_receivable",
        documentStatus: String = "not_required",
        createdAt: Instant = Instant.parse("2026-05-15T12:00:00Z"),
    ): Document = root(id, organizationId, createdAt)
        .append("branchId", BRANCH_ID)
        .append("activityId", ACTIVITY_ID)
        .append("saleNumber", saleNumber)
        .append("saleType", "standard_sale")
        .append("workflowMode", "quick_sale")
        .append("operationalStatus", operationalStatus)
        .append("paymentStatus", paymentStatus)
        .append("collectionStatus", collectionStatus)
        .append("documentStatus", documentStatus)
        .append("customerId", customerId)
        .append("customerSnapshot", Document("displayName", "Cliente Prueba"))
        .append(
            "items",
            listOf(
                Document("lineId", "line_1")
                    .append("catalogItemId", CATALOG_ITEM_ID)
                    .append("name", "Cuy entero")
                    .append("quantity", decimal("1.000000"))
                    .append("unitCode", "unit")
                    .append("unitPrice", money("24.00"))
                    .append("lineTotal", money("24.00"))
                    .append("status", "pending")
            )
        )
        .append("totals", Document("grandTotal", money("24.00")).append("currency", "USD"))
        .append("taxSummary", Document("totalTax", money("3.60")).append("subtotalTaxable", money("24.00")))
        .append("paymentRefs", emptyList<Document>())
        .append("documentRefs", emptyList<Document>())
        .append("reservationRef", null)
        .append("cashSessionId", CASH_SESSION_ID)
        .append("confirmedAt", null)
        .append("closedAt", null)
        .append("canceledAt", null)

    fun paymentDocument(
        id: String,
        organizationId: String = ORGANIZATION_ID,
        saleId: String? = SALE_ID,
        cashSessionId: String? = CASH_SESSION_ID,
        externalReference: String? = null,
        paidAt: Instant = Instant.parse("2026-05-15T12:10:00Z"),
        amount: String = "24.00",
    ): Document = root(id, organizationId, paidAt)
        .append("branchId", BRANCH_ID)
        .append("saleId", saleId)
        .append("customerId", CUSTOMER_ID)
        .append("cashSessionId", cashSessionId)
        .append("method", "cash")
        .append("status", "confirmed")
        .append("amount", money(amount))
        .append("paidAt", Date.from(paidAt))
        .append("externalReference", externalReference)
        .append("allocations", listOf(Document("saleId", saleId).append("amount", money(amount))))

    fun receivableDocument(
        id: String,
        organizationId: String = ORGANIZATION_ID,
        saleId: String = SALE_ID,
        customerId: String = CUSTOMER_ID,
        status: String = "open",
        dueAt: Instant? = Instant.parse("2026-05-20T12:00:00Z"),
    ): Document = root(id, organizationId)
        .append("branchId", BRANCH_ID)
        .append("saleId", saleId)
        .append("customerId", customerId)
        .append("status", status)
        .append("originalAmount", money("24.00"))
        .append("paidAmount", money(if (status == "partially_collected") "10.00" else "0.00"))
        .append("balanceDue", money(if (status == "partially_collected") "14.00" else "24.00"))
        .append("dueAt", dueAt?.let { Date.from(it) })
        .append("settledAt", null)
        .append("paymentRefs", emptyList<Document>())

    fun cashSessionDocument(
        id: String = CASH_SESSION_ID,
        organizationId: String = ORGANIZATION_ID,
        branchId: String = BRANCH_ID,
        status: String = "open",
        openedBy: String = USER_ID,
        openedAt: Instant = Instant.parse("2026-05-15T11:00:00Z"),
    ): Document = root(id, organizationId, openedAt)
        .append("branchId", branchId)
        .append("openedBy", openedBy)
        .append("openedAt", Date.from(openedAt))
        .append("status", status)
        .append("openingBalance", money("50.00"))
        .append("expectedCashAmount", money("74.00"))
        .append("countedCashAmount", money("74.00"))
        .append("differenceAmount", money("0.00"))
        .append("closingStartedAt", null)
        .append("closedAt", null)
        .append("canceledAt", null)
        .append("summary", Document("payments", 1))

    fun cashMovementDocument(
        id: String,
        type: String,
        direction: String,
        amount: String,
        occurredAt: Instant,
        referenceType: String? = null,
        referenceId: String? = null,
    ): Document = root(id, ORGANIZATION_ID, occurredAt)
        .append("cashSessionId", CASH_SESSION_ID)
        .append("branchId", BRANCH_ID)
        .append("type", type)
        .append("direction", direction)
        .append("amount", money(amount))
        .append("occurredAt", Date.from(occurredAt))
        .append("referenceType", referenceType)
        .append("referenceId", referenceId)
        .append("notes", null)

    fun commercialDocument(
        id: String = DOCUMENT_ID,
        organizationId: String = ORGANIZATION_ID,
        saleId: String? = SALE_ID,
        documentNumber: String = "001-001-000000001",
        accessKey: String? = ACCESS_KEY,
        status: String = "authorized",
        issuedAt: Instant = Instant.parse("2026-05-15T12:20:00Z"),
    ): Document = root(id, organizationId, issuedAt)
        .append("branchId", BRANCH_ID)
        .append("emissionPointId", "emi_phase43_001")
        .append("saleId", saleId)
        .append("customerId", CUSTOMER_ID)
        .append("documentType", "electronic_invoice")
        .append("documentNumber", documentNumber)
        .append("accessKey", accessKey)
        .append("authorizationNumber", "AUTH-001")
        .append("status", status)
        .append("issuedAt", Date.from(issuedAt))
        .append("authorizedAt", Date.from(issuedAt.plusSeconds(30)))
        .append("totalsSnapshot", Document("grandTotal", money("24.00")))
        .append("taxSnapshot", Document("iva", money("3.60")))
        .append("payloadId", PAYLOAD_ID)

    fun electronicPayload(
        id: String = PAYLOAD_ID,
        documentId: String = DOCUMENT_ID,
        status: String = "authorized",
    ): Document = root(id)
        .append("documentId", documentId)
        .append("xmlUnsignedObjectKey", "storage://xml/unsigned.xml")
        .append("xmlSignedObjectKey", "storage://xml/signed.xml")
        .append("rideObjectKey", "storage://ride/invoice.pdf")
        .append("schemaVersionCode", "factura_V2.1.0")
        .append("signatureId", "sig_phase43_001")
        .append("signedAt", Date.from(Instant.parse("2026-05-15T12:21:00Z")))
        .append("status", status)
        .append("checksums", Document("sha256", "abc123"))
        .append("accessKey", ACCESS_KEY)

    fun sriSubmission(
        id: String,
        status: String,
        requestAt: Instant,
        payloadId: String? = PAYLOAD_ID,
    ): Document = root(id, ORGANIZATION_ID, requestAt)
        .append("documentId", DOCUMENT_ID)
        .append("payloadId", payloadId)
        .append("accessKey", ACCESS_KEY)
        .append("environment", "test")
        .append("submissionType", "authorization")
        .append("status", status)
        .append("requestAt", Date.from(requestAt))
        .append("submittedAt", Date.from(requestAt))
        .append("responseAt", Date.from(requestAt.plusSeconds(3)))
        .append("responseCode", "OK")
        .append("responseMessage", "Autorizado")
        .append("rawResponseObjectKey", "storage://sri/response.json")

    fun outboxEvent(
        id: String,
        eventId: String,
        status: String,
        availableAt: Instant,
        aggregateType: String = "sale",
        aggregateId: String = SALE_ID,
    ): Document = root(id, ORGANIZATION_ID, availableAt)
        .append("eventId", eventId)
        .append("eventType", "SaleCreated")
        .append("aggregateType", aggregateType)
        .append("aggregateId", aggregateId)
        .append("payload", Document("saleId", aggregateId))
        .append("status", status)
        .append("availableAt", Date.from(availableAt))
        .append("publishedAt", null)
        .append("attempts", 0)
        .append("lastError", null)

    fun auditLog(
        id: String,
        action: String,
        entityType: String,
        entityId: String,
        actorUserId: String? = USER_ID,
        occurredAt: Instant = Instant.parse("2026-05-15T12:30:00Z"),
    ): Document = root(id, ORGANIZATION_ID, occurredAt)
        .append("actorUserId", actorUserId)
        .append("actorType", "user")
        .append("action", action)
        .append("entityType", entityType)
        .append("entityId", entityId)
        .append("occurredAt", Date.from(occurredAt))
        .append("ipAddress", "127.0.0.1")
        .append("requestId", "req_phase43")
        .append("reason", null)
        .append("before", Document())
        .append("after", Document("status", "pending"))
        .append("metadata", Document("source", "test"))

    fun domainEvent(
        id: String,
        sequence: Int,
        eventType: String,
        aggregateType: String = "sale",
        aggregateId: String = SALE_ID,
        occurredAt: Instant = Instant.parse("2026-05-15T12:31:00Z"),
    ): Document = root(id, ORGANIZATION_ID, occurredAt)
        .append("aggregateType", aggregateType)
        .append("aggregateId", aggregateId)
        .append("eventType", eventType)
        .append("eventVersion", 1)
        .append("sequence", sequence)
        .append("occurredAt", Date.from(occurredAt))
        .append("payload", Document("aggregateId", aggregateId))
        .append("metadata", Document("source", "test"))

    fun stockBalance(
        id: String,
        status: String = "available",
        quantityAvailable: String = "10.000000",
    ): Document = root(id)
        .append("branchId", BRANCH_ID)
        .append("catalogItemId", CATALOG_ITEM_ID)
        .append("stockUnit", "unit")
        .append("quantityOnHand", decimal("10.000000"))
        .append("quantityReserved", decimal("0.000000"))
        .append("quantityAvailable", decimal(quantityAvailable))
        .append("lowStockThreshold", decimal("2.000000"))
        .append("status", status)
        .append("lastMovementAt", null)
        .append("locationCode", null)

    fun stockMovement(
        id: String,
        type: String,
        direction: String,
        quantity: String,
        occurredAt: Instant,
        referenceType: String? = null,
        referenceId: String? = null,
    ): Document = root(id, ORGANIZATION_ID, occurredAt)
        .append("branchId", BRANCH_ID)
        .append("catalogItemId", CATALOG_ITEM_ID)
        .append("type", type)
        .append("direction", direction)
        .append("quantity", decimal(quantity))
        .append("unitCode", "unit")
        .append("occurredAt", Date.from(occurredAt))
        .append("referenceType", referenceType)
        .append("referenceId", referenceId)
        .append("reason", "repository test")

    fun serviceOrder(
        id: String,
        saleId: String? = SALE_ID,
        status: String = "confirmed",
        scheduledStartAt: Instant? = Instant.parse("2026-05-16T10:00:00Z"),
    ): Document = root(id)
        .append("saleId", saleId)
        .append("branchId", BRANCH_ID)
        .append("activityId", ACTIVITY_ID)
        .append("customerId", CUSTOMER_ID)
        .append("status", status)
        .append("scheduledStartAt", scheduledStartAt?.let { Date.from(it) })
        .append("scheduledEndAt", scheduledStartAt?.plusSeconds(3600)?.let { Date.from(it) })
        .append("metadata", Document("source", "test"))

    fun reservation(
        id: String,
        saleId: String? = SALE_ID,
        status: String = "confirmed",
        startAt: Instant = Instant.parse("2026-05-16T10:00:00Z"),
        customerId: String? = CUSTOMER_ID,
    ): Document = root(id, ORGANIZATION_ID, startAt)
        .append("saleId", saleId)
        .append("branchId", BRANCH_ID)
        .append("activityId", ACTIVITY_ID)
        .append("customerId", customerId)
        .append("resourceId", null)
        .append("startAt", Date.from(startAt))
        .append("endAt", Date.from(startAt.plusSeconds(3600)))
        .append("partySize", 2)
        .append("status", status)
        .append("notes", null)

}
