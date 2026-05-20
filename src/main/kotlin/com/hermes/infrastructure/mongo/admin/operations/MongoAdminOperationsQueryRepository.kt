package com.hermes.infrastructure.mongo.admin.operations

import com.hermes.application.admin.operations.*
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.Decimal128
import java.math.BigDecimal
import java.time.Instant
import java.util.*

class MongoAdminOperationsQueryRepository(
    database: MongoDatabase,
) : AdminOperationsQueryRepository {
    private val sales: MongoCollection<Document> = database.getCollection(MongoCollectionNames.SALES)
    private val payments: MongoCollection<Document> = database.getCollection(MongoCollectionNames.PAYMENTS)
    private val receivables: MongoCollection<Document> = database.getCollection(MongoCollectionNames.RECEIVABLES)
    private val cashSessions: MongoCollection<Document> = database.getCollection(MongoCollectionNames.CASH_SESSIONS)
    private val cashMovements: MongoCollection<Document> = database.getCollection(MongoCollectionNames.CASH_MOVEMENTS)
    private val commercialDocuments: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.COMMERCIAL_DOCUMENTS)

    override fun searchSales(command: SearchAdminSalesCommand): List<AdminSaleListItem> {
        val filters = saleFilters(
            organizationId = command.organizationId,
            branchId = command.branchId,
            activityId = command.activityId,
            customerId = command.customerId,
            operationalStatuses = command.operationalStatuses,
            paymentStatuses = command.paymentStatuses,
            saleTypes = command.saleTypes,
            from = command.from,
            to = command.to,
            query = command.query,
        )
        return sales.find(Filters.and(filters)).sort(Sorts.descending(MongoDocumentFields.CREATED_AT))
            .limit(command.limit.coerceIn(1, 250)).into(mutableListOf()).map(::saleListItemFromDocument)
    }

    override fun findSale(command: GetAdminSaleCommand): AdminSaleDetail? {
        val raw = sales.find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, command.organizationId.trim()),
                Filters.eq(MongoDocumentFields.ID, command.saleId.trim()),
            ),
        ).firstOrNull() ?: return null

        val summary = saleListItemFromDocument(raw)
        val lines = raw.documentList("items").map(::saleLineFromDocument)
        val embeddedPayments = raw.documentList("payments").map(::embeddedPaymentFromDocument)
        val storedPayments = payments.find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, command.organizationId.trim()),
                Filters.eq("saleId", command.saleId.trim()),
            ),
        ).sort(Sorts.descending("paidAt")).into(mutableListOf()).map(::paymentFromDocument)
        val documents = commercialDocuments.find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, command.organizationId.trim()),
                Filters.eq("saleId", command.saleId.trim()),
            ),
        ).sort(Sorts.descending("issuedAt")).into(mutableListOf()).map(::commercialDocumentFromDocument)

        return AdminSaleDetail(
            summary = summary,
            lines = lines,
            payments = (storedPayments.ifEmpty { embeddedPayments }),
            documents = documents,
        )
    }

    override fun searchCashSessions(command: SearchAdminCashSessionsCommand): List<AdminCashSessionReadModel> {
        val filters =
            mutableListOf<Bson>(Filters.eq(MongoDocumentFields.ORGANIZATION_ID, command.organizationId.trim()))
        command.branchId?.takeIfNotBlank()?.let { filters += Filters.eq("branchId", it) }
        if (command.statuses.isNotEmpty()) filters += Filters.`in`("status", command.statuses.normalizedStorageValues())
        command.from?.let { filters += Filters.gte("openedAt", it.toDate()) }
        command.to?.let { filters += Filters.lt("openedAt", it.toDate()) }

        return cashSessions.find(Filters.and(filters)).sort(Sorts.descending("openedAt"))
            .limit(command.limit.coerceIn(1, 250)).into(mutableListOf())
            .map { cashSessionFromDocument(it, includeMovements = false) }
    }

    override fun findCurrentCashSession(command: GetCurrentAdminCashSessionCommand): AdminCashSessionReadModel? {
        val filters = mutableListOf<Bson>(
            Filters.eq(MongoDocumentFields.ORGANIZATION_ID, command.organizationId.trim()),
            Filters.eq("status", "open"),
        )
        command.branchId?.takeIfNotBlank()?.let { filters += Filters.eq("branchId", it) }
        return cashSessions.find(Filters.and(filters)).sort(Sorts.descending("openedAt")).firstOrNull()
            ?.let { cashSessionFromDocument(it, includeMovements = true) }
    }

    override fun findCashSession(command: GetAdminCashSessionCommand): AdminCashSessionReadModel? = cashSessions.find(
        Filters.and(
            Filters.eq(MongoDocumentFields.ORGANIZATION_ID, command.organizationId.trim()),
            Filters.eq(MongoDocumentFields.ID, command.cashSessionId.trim()),
        ),
    ).firstOrNull()?.let { cashSessionFromDocument(it, includeMovements = true) }

    override fun searchPayments(command: SearchAdminPaymentsCommand): List<AdminPaymentReadModel> {
        val filters =
            mutableListOf<Bson>(Filters.eq(MongoDocumentFields.ORGANIZATION_ID, command.organizationId.trim()))
        command.branchId?.takeIfNotBlank()?.let { filters += Filters.eq("branchId", it) }
        command.saleId?.takeIfNotBlank()?.let { filters += Filters.eq("saleId", it) }
        command.customerId?.takeIfNotBlank()?.let { filters += Filters.eq("customerId", it) }
        command.cashSessionId?.takeIfNotBlank()?.let { filters += Filters.eq("cashSessionId", it) }
        if (command.methods.isNotEmpty()) filters += Filters.`in`("method", command.methods.normalizedStorageValues())
        if (command.statuses.isNotEmpty()) filters += Filters.`in`("status", command.statuses.normalizedStorageValues())
        command.from?.let { filters += Filters.gte("paidAt", it.toDate()) }
        command.to?.let { filters += Filters.lt("paidAt", it.toDate()) }

        return payments.find(Filters.and(filters)).sort(Sorts.descending("paidAt"))
            .limit(command.limit.coerceIn(1, 300)).into(mutableListOf()).map(::paymentFromDocument)
    }

    override fun searchReceivables(command: SearchAdminReceivablesCommand): List<AdminReceivableReadModel> {
        val filters =
            mutableListOf<Bson>(Filters.eq(MongoDocumentFields.ORGANIZATION_ID, command.organizationId.trim()))
        command.branchId?.takeIfNotBlank()?.let { filters += Filters.eq("branchId", it) }
        command.customerId?.takeIfNotBlank()?.let { filters += Filters.eq("customerId", it) }
        if (command.statuses.isNotEmpty()) filters += Filters.`in`("status", command.statuses.normalizedStorageValues())
        command.dueFrom?.let { filters += Filters.gte("dueAt", it.toDate()) }
        command.dueTo?.let { filters += Filters.lt("dueAt", it.toDate()) }

        return receivables.find(Filters.and(filters)).sort(Sorts.ascending("dueAt", MongoDocumentFields.CREATED_AT))
            .limit(command.limit.coerceIn(1, 300)).into(mutableListOf()).map(::receivableFromDocument)
    }

    override fun operationalToday(command: GetAdminOperationalTodayReportCommand): AdminOperationalTodayReport {
        val salesSummary = salesSummary(
            GetAdminSalesSummaryReportCommand(
                organizationId = command.organizationId,
                actorUserId = command.actorUserId,
                actorEffectivePermissions = command.actorEffectivePermissions,
                branchId = command.branchId,
                activityId = command.activityId,
                from = command.from,
                to = command.to,
            ),
        )
        val cashSummary = cashSummary(
            GetAdminCashSummaryReportCommand(
                organizationId = command.organizationId,
                actorUserId = command.actorUserId,
                actorEffectivePermissions = command.actorEffectivePermissions,
                branchId = command.branchId,
                from = command.from,
                to = command.to,
            ),
        )
        val taxSummary = taxSummary(
            GetAdminTaxSummaryReportCommand(
                organizationId = command.organizationId,
                actorUserId = command.actorUserId,
                actorEffectivePermissions = command.actorEffectivePermissions,
                branchId = command.branchId,
                activityId = command.activityId,
                from = command.from,
                to = command.to,
            ),
        )
        val currentCash = findCurrentCashSession(
            GetCurrentAdminCashSessionCommand(
                organizationId = command.organizationId,
                actorUserId = command.actorUserId,
                actorEffectivePermissions = command.actorEffectivePermissions,
                branchId = command.branchId,
            ),
        )

        val pendingReceivables = searchReceivables(
            SearchAdminReceivablesCommand(
                organizationId = command.organizationId,
                actorUserId = command.actorUserId,
                actorEffectivePermissions = command.actorEffectivePermissions,
                branchId = command.branchId,
                statuses = setOf("open", "partially_collected", "overdue"),
                limit = 300,
            ),
        ).fold(AdminMoneyAmount.zero()) { acc, receivable -> acc.safePlus(receivable.balanceDue) }

        val alerts = buildList {
            if (currentCash == null) {
                add(
                    AdminOperationalAlert(
                        code = "cash_session_not_open",
                        severity = "warning",
                        message = "No hay una caja abierta para el contexto seleccionado.",
                        actionHint = "Abrir caja antes de registrar cobros en efectivo.",
                    ),
                )
            }
            if (pendingReceivables.amount.signum() > 0) {
                add(
                    AdminOperationalAlert(
                        code = "pending_receivables",
                        severity = "info",
                        message = "Existen cuentas por cobrar pendientes.",
                        actionHint = "Revisar el módulo de pendientes por cobrar.",
                    ),
                )
            }
            if (salesSummary.canceledSaleCount > 0) {
                add(
                    AdminOperationalAlert(
                        code = "canceled_sales_today",
                        severity = "info",
                        message = "Hoy existen ventas canceladas.",
                        actionHint = "Revisar motivos de cancelación y auditoría.",
                    ),
                )
            }
            if (taxSummary.documentCount > taxSummary.authorizedDocumentCount) {
                add(
                    AdminOperationalAlert(
                        code = "documents_not_authorized",
                        severity = "warning",
                        message = "Hay documentos comerciales no autorizados o pendientes.",
                        actionHint = "Revisar facturación electrónica/documentos.",
                    ),
                )
            }
        }

        return AdminOperationalTodayReport(
            organizationId = command.organizationId,
            branchId = command.branchId,
            activityId = command.activityId,
            businessDate = command.businessDate,
            from = command.from,
            to = command.to,
            sales = salesSummary,
            cash = cashSummary,
            tax = taxSummary,
            currentCashSession = currentCash,
            pendingReceivables = pendingReceivables,
            topItems = salesSummary.topItems,
            alerts = alerts,
        )
    }

    override fun salesSummary(command: GetAdminSalesSummaryReportCommand): AdminSalesSummaryReport {
        val docs = sales.find(
            Filters.and(
                saleFilters(
                    organizationId = command.organizationId,
                    branchId = command.branchId,
                    activityId = command.activityId,
                    from = command.from,
                    to = command.to,
                ),
            ),
        ).sort(Sorts.descending(MongoDocumentFields.CREATED_AT)).into(mutableListOf())

        val summaries = docs.map(::saleListItemFromDocument)
        val activeSummaries = summaries.filterNot { it.operationalStatus == "canceled" }
        val currency = activeSummaries.firstOrNull()?.grandTotal?.currency ?: "USD"
        val topItems = aggregateTopItems(docs)

        return AdminSalesSummaryReport(
            organizationId = command.organizationId,
            branchId = command.branchId,
            activityId = command.activityId,
            from = command.from,
            to = command.to,
            saleCount = summaries.size,
            closedSaleCount = summaries.count { it.operationalStatus == "closed" },
            canceledSaleCount = summaries.count { it.operationalStatus == "canceled" },
            openSaleCount = summaries.count { it.operationalStatus !in setOf("closed", "canceled") },
            itemCount = activeSummaries.sumOf { it.itemCount },
            subtotal = activeSummaries.fold(AdminMoneyAmount.zero(currency)) { acc, item -> acc.safePlus(item.subtotal) },
            discountTotal = activeSummaries.fold(AdminMoneyAmount.zero(currency)) { acc, item -> acc.safePlus(item.discountTotal) },
            taxTotal = activeSummaries.fold(AdminMoneyAmount.zero(currency)) { acc, item -> acc.safePlus(item.taxTotal) },
            grandTotal = activeSummaries.fold(AdminMoneyAmount.zero(currency)) { acc, item -> acc.safePlus(item.grandTotal) },
            paidTotal = activeSummaries.fold(AdminMoneyAmount.zero(currency)) { acc, item -> acc.safePlus(item.paidAmount) },
            receivableTotal = activeSummaries.fold(AdminMoneyAmount.zero(currency)) { acc, item -> acc.safePlus(item.receivableAmount) },
            byOperationalStatus = summaries.statusCounts { it.operationalStatus },
            byPaymentStatus = summaries.statusCounts { it.paymentStatus },
            byDocumentStatus = summaries.statusCounts { it.documentStatus ?: "unknown" },
            topItems = topItems,
        )
    }

    override fun cashSummary(command: GetAdminCashSummaryReportCommand): AdminCashSummaryReport {
        val sessionFilters =
            mutableListOf<Bson>(Filters.eq(MongoDocumentFields.ORGANIZATION_ID, command.organizationId.trim()))
        command.branchId?.takeIfNotBlank()?.let { sessionFilters += Filters.eq("branchId", it) }
        command.from.let { sessionFilters += Filters.gte("openedAt", it.toDate()) }
        command.to.let { sessionFilters += Filters.lt("openedAt", it.toDate()) }
        val sessionDocs = cashSessions.find(Filters.and(sessionFilters)).into(mutableListOf())
            .map { cashSessionFromDocument(it, includeMovements = false) }

        val movementFilters =
            mutableListOf<Bson>(Filters.eq(MongoDocumentFields.ORGANIZATION_ID, command.organizationId.trim()))
        command.branchId?.takeIfNotBlank()?.let { movementFilters += Filters.eq("branchId", it) }
        movementFilters += Filters.gte("occurredAt", command.from.toDate())
        movementFilters += Filters.lt("occurredAt", command.to.toDate())
        val movementDocs =
            cashMovements.find(Filters.and(movementFilters)).into(mutableListOf()).map(::cashMovementFromDocument)

        val currency =
            (movementDocs.firstOrNull()?.amount ?: sessionDocs.firstOrNull()?.expectedCashAmount)?.currency ?: "USD"
        val cashIn = movementDocs.filter { it.direction == "in" }
            .fold(AdminMoneyAmount.zero(currency)) { acc, item -> acc.safePlus(item.amount) }
        val cashOut = movementDocs.filter { it.direction == "out" }
            .fold(AdminMoneyAmount.zero(currency)) { acc, item -> acc.safePlus(item.amount) }

        return AdminCashSummaryReport(
            organizationId = command.organizationId,
            branchId = command.branchId,
            from = command.from,
            to = command.to,
            openSessionCount = sessionDocs.count { it.status == "open" },
            closedSessionCount = sessionDocs.count { it.status == "closed" },
            movementCount = movementDocs.size,
            cashInTotal = cashIn,
            cashOutTotal = cashOut,
            netCashMovement = AdminMoneyAmount(cashIn.amount - cashOut.amount, currency),
            expectedOpenCashTotal = sessionDocs.filter { it.status == "open" }
                .fold(AdminMoneyAmount.zero(currency)) { acc, item -> acc.safePlus(item.expectedCashAmount) },
            countedClosedCashTotal = sessionDocs.filter { it.status == "closed" }
                .fold(AdminMoneyAmount.zero(currency)) { acc, item ->
                    acc.safePlus(
                        item.countedCashAmount ?: AdminMoneyAmount.zero(currency)
                    )
                },
            differenceClosedCashTotal = sessionDocs.filter { it.status == "closed" }
                .fold(AdminMoneyAmount.zero(currency)) { acc, item ->
                    acc.safePlus(
                        item.differenceAmount ?: AdminMoneyAmount.zero(currency)
                    )
                },
            byMovementType = movementDocs.statusCounts { it.type },
        )
    }

    override fun taxSummary(command: GetAdminTaxSummaryReportCommand): AdminTaxSummaryReport {
        val filters =
            mutableListOf<Bson>(Filters.eq(MongoDocumentFields.ORGANIZATION_ID, command.organizationId.trim()))
        command.branchId?.takeIfNotBlank()?.let { filters += Filters.eq("branchId", it) }
        command.from.let { filters += Filters.gte("issuedAt", it.toDate()) }
        command.to.let { filters += Filters.lt("issuedAt", it.toDate()) }

        val docs = commercialDocuments.find(Filters.and(filters)).into(mutableListOf()).filter { document ->
            command.activityId?.takeIfNotBlank()?.let { activityId ->
                val saleId = document.getString("saleId") ?: return@filter false
                sales.find(
                    Filters.and(
                        Filters.eq(MongoDocumentFields.ORGANIZATION_ID, command.organizationId.trim()),
                        Filters.eq(MongoDocumentFields.ID, saleId),
                        Filters.eq("activityId", activityId),
                    ),
                ).firstOrNull() != null
            } ?: true
        }
        val mappedDocs = docs.map(::commercialDocumentFromDocument)
        val currency = mappedDocs.firstOrNull()?.grandTotal?.currency ?: "USD"
        val taxLines = aggregateTaxLines(docs, currency)

        return AdminTaxSummaryReport(
            organizationId = command.organizationId,
            branchId = command.branchId,
            activityId = command.activityId,
            from = command.from,
            to = command.to,
            documentCount = mappedDocs.size,
            authorizedDocumentCount = mappedDocs.count { it.status == "authorized" || it.status == "approved" },
            documentGrandTotal = mappedDocs.fold(AdminMoneyAmount.zero(currency)) { acc, item -> acc.safePlus(item.grandTotal) },
            taxTotal = mappedDocs.fold(AdminMoneyAmount.zero(currency)) { acc, item -> acc.safePlus(item.taxTotal) },
            byTaxRate = taxLines,
        )
    }

    private fun saleFilters(
        organizationId: String,
        branchId: String? = null,
        activityId: String? = null,
        customerId: String? = null,
        operationalStatuses: Set<String> = emptySet(),
        paymentStatuses: Set<String> = emptySet(),
        saleTypes: Set<String> = emptySet(),
        from: Instant? = null,
        to: Instant? = null,
        query: String? = null,
    ): MutableList<Bson> {
        val filters = mutableListOf<Bson>(Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()))
        branchId?.takeIfNotBlank()?.let { filters += Filters.eq("branchId", it) }
        activityId?.takeIfNotBlank()?.let { filters += Filters.eq("activityId", it) }
        customerId?.takeIfNotBlank()?.let { filters += Filters.eq("customerId", it) }
        if (operationalStatuses.isNotEmpty()) filters += Filters.`in`(
            "operationalStatus", operationalStatuses.normalizedStorageValues()
        )
        if (paymentStatuses.isNotEmpty()) filters += Filters.`in`(
            "paymentStatus", paymentStatuses.normalizedStorageValues()
        )
        if (saleTypes.isNotEmpty()) filters += Filters.`in`("saleType", saleTypes.normalizedStorageValues())
        from?.let { filters += Filters.gte(MongoDocumentFields.CREATED_AT, it.toDate()) }
        to?.let { filters += Filters.lt(MongoDocumentFields.CREATED_AT, it.toDate()) }
        query?.takeIfNotBlank()?.let { raw ->
            val pattern = Regex.escape(raw)
            filters += Filters.or(
                Filters.regex("saleNumber", pattern, "i"),
                Filters.regex("customerSnapshot.displayName", pattern, "i"),
                Filters.regex("customerSnapshot.taxId", pattern, "i"),
            )
        }
        return filters
    }

    private fun saleListItemFromDocument(raw: Document): AdminSaleListItem {
        val totals = raw.optionalDocument("totals")
        val items = raw.documentList("items")
        val grandTotal = totals.moneyOrZero("grandTotal")
        val paid = raw.documentList("payments").filter {
            (it.optionalString("status") ?: "confirmed") !in setOf(
                "voided", "reversed", "refunded", "canceled"
            )
        }.fold(AdminMoneyAmount.zero(grandTotal.currency)) { acc, payment -> acc.safePlus(payment.money("amount")) }
        val customer = raw.optionalDocument("customerSnapshot")
        return AdminSaleListItem(
            id = raw.requiredString(MongoDocumentFields.ID),
            organizationId = raw.requiredString(MongoDocumentFields.ORGANIZATION_ID),
            branchId = raw.optionalString("branchId"),
            activityId = raw.optionalString("activityId"),
            saleNumber = raw.optionalString("saleNumber"),
            saleType = raw.optionalString("saleType") ?: "standard_sale",
            customerId = raw.optionalString("customerId") ?: customer?.optionalString("customerId"),
            customerDisplayName = customer?.optionalString("displayName"),
            operationalStatus = raw.optionalString("operationalStatus") ?: "unknown",
            paymentStatus = raw.optionalString("paymentStatus") ?: "unknown",
            documentStatus = raw.optionalString("documentStatus"),
            itemCount = items.count { it.optionalString("status") != "canceled" },
            subtotal = totals.moneyOrZero("subtotal"),
            discountTotal = totals.moneyOrZero("discount"),
            taxTotal = totals.moneyOrZero("taxTotal"),
            grandTotal = grandTotal,
            paidAmount = paid,
            receivableAmount = grandTotal.safeMinus(paid).positiveOrZero(),
            dueAt = raw.instantOrNull("dueAt"),
            cashSessionId = raw.optionalString("cashSessionId"),
            createdAt = raw.instantOrNow(MongoDocumentFields.CREATED_AT),
            updatedAt = raw.instantOrNow(MongoDocumentFields.UPDATED_AT),
        )
    }

    private fun saleLineFromDocument(raw: Document): AdminSaleLineReadModel {
        val taxProfile = raw.optionalDocument("taxProfileSnapshot")
        val quantity = raw.optionalDocument("quantity")?.decimal("value") ?: BigDecimal.ONE
        return AdminSaleLineReadModel(
            id = raw.requiredString("id"),
            catalogItemId = raw.optionalString("catalogItemId"),
            name = raw.optionalString("name") ?: raw.optionalString("description") ?: "Item",
            quantity = quantity,
            unitCode = raw.optionalDocument("quantity")?.optionalString("unitCode") ?: raw.optionalString("unitCode"),
            unitPrice = raw.money("unitPrice"),
            discount = raw.money("discount"),
            netTotal = raw.money("netTotal"),
            taxTotal = raw.money("taxTotal"),
            lineTotal = raw.money("lineTotal"),
            status = raw.optionalString("status") ?: "unknown",
            taxProfileCode = taxProfile?.optionalString("code"),
            sriTaxCode = taxProfile?.optionalString("sriTaxCode"),
            sriRateCode = taxProfile?.optionalString("sriRateCode"),
        )
    }

    private fun embeddedPaymentFromDocument(raw: Document): AdminPaymentReadModel = AdminPaymentReadModel(
        id = raw.optionalString("id") ?: raw.optionalString(MongoDocumentFields.ID) ?: "payment_unknown",
        organizationId = raw.optionalString(MongoDocumentFields.ORGANIZATION_ID) ?: raw.optionalString("organizationId")
        ?: "",
        branchId = raw.optionalString("branchId"),
        saleId = raw.optionalString("saleId"),
        customerId = raw.optionalString("customerId"),
        cashSessionId = raw.optionalString("cashSessionId"),
        amount = raw.money("amount"),
        method = raw.optionalString("method") ?: "unknown",
        status = raw.optionalString("status") ?: "unknown",
        paidAt = raw.instantOrNow("paidAt"),
        externalReference = raw.optionalString("externalReference") ?: raw.optionalString("reference"),
        notes = raw.optionalString("notes"),
    )

    private fun paymentFromDocument(raw: Document): AdminPaymentReadModel = AdminPaymentReadModel(
        id = raw.requiredString(MongoDocumentFields.ID),
        organizationId = raw.requiredString(MongoDocumentFields.ORGANIZATION_ID),
        branchId = raw.optionalString("branchId"),
        saleId = raw.optionalString("saleId"),
        customerId = raw.optionalString("customerId"),
        cashSessionId = raw.optionalString("cashSessionId"),
        amount = raw.money("amount"),
        method = raw.optionalString("method") ?: "unknown",
        status = raw.optionalString("status") ?: "unknown",
        paidAt = raw.instantOrNow("paidAt"),
        externalReference = raw.optionalString("externalReference") ?: raw.optionalString("reference"),
        notes = raw.optionalString("notes"),
    )

    private fun cashSessionFromDocument(raw: Document, includeMovements: Boolean): AdminCashSessionReadModel {
        val id = raw.requiredString(MongoDocumentFields.ID)
        val organizationId = raw.requiredString(MongoDocumentFields.ORGANIZATION_ID)
        val movements = if (includeMovements) {
            cashMovements.find(
                Filters.and(
                    Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId),
                    Filters.eq("cashSessionId", id),
                ),
            ).sort(Sorts.ascending("occurredAt")).into(mutableListOf()).map(::cashMovementFromDocument)
        } else emptyList()
        val summary = raw.optionalDocument("summary")
        return AdminCashSessionReadModel(
            id = id,
            organizationId = organizationId,
            branchId = raw.optionalString("branchId"),
            openedBy = raw.optionalString("openedBy"),
            openedAt = raw.instantOrNow("openedAt"),
            status = raw.optionalString("status") ?: "unknown",
            openingBalance = raw.money("openingBalance"),
            expectedCashAmount = raw.money("expectedCashAmount"),
            countedCashAmount = raw.optionalDocument("countedCashAmount")?.toMoney(),
            differenceAmount = raw.optionalDocument("differenceAmount")?.toMoney(),
            movementCount = summary?.intOrNull("movementCount") ?: movements.size,
            closingStartedAt = raw.instantOrNull("closingStartedAt"),
            closedAt = raw.instantOrNull("closedAt"),
            canceledAt = raw.instantOrNull("canceledAt"),
            movements = movements,
        )
    }

    private fun cashMovementFromDocument(raw: Document): AdminCashMovementReadModel = AdminCashMovementReadModel(
        id = raw.requiredString(MongoDocumentFields.ID),
        organizationId = raw.requiredString(MongoDocumentFields.ORGANIZATION_ID),
        cashSessionId = raw.requiredString("cashSessionId"),
        branchId = raw.optionalString("branchId"),
        type = raw.optionalString("type") ?: "unknown",
        direction = raw.optionalString("direction") ?: "neutral",
        amount = raw.money("amount"),
        occurredAt = raw.instantOrNow("occurredAt"),
        referenceType = raw.optionalString("referenceType"),
        referenceId = raw.optionalString("referenceId"),
        notes = raw.optionalString("notes"),
    )

    private fun receivableFromDocument(raw: Document): AdminReceivableReadModel = AdminReceivableReadModel(
        id = raw.requiredString(MongoDocumentFields.ID),
        organizationId = raw.requiredString(MongoDocumentFields.ORGANIZATION_ID),
        branchId = raw.optionalString("branchId"),
        saleId = raw.requiredString("saleId"),
        customerId = raw.optionalString("customerId"),
        status = raw.optionalString("status") ?: "open",
        totalDue = raw.money("originalAmount"),
        paidAmount = raw.money("paidAmount"),
        balanceDue = raw.money("balanceDue"),
        dueAt = raw.instantOrNull("dueAt"),
        settledAt = raw.instantOrNull("settledAt"),
        createdAt = raw.instantOrNow(MongoDocumentFields.CREATED_AT),
        updatedAt = raw.instantOrNow(MongoDocumentFields.UPDATED_AT),
    )

    private fun commercialDocumentFromDocument(raw: Document): AdminCommercialDocumentReadModel {
        val totals = raw.optionalDocument("totalsSnapshot")
        return AdminCommercialDocumentReadModel(
            id = raw.requiredString(MongoDocumentFields.ID),
            organizationId = raw.requiredString(MongoDocumentFields.ORGANIZATION_ID),
            branchId = raw.optionalString("branchId"),
            emissionPointId = raw.optionalString("emissionPointId"),
            saleId = raw.optionalString("saleId"),
            customerId = raw.optionalString("customerId"),
            documentType = raw.optionalString("documentType") ?: "unknown",
            documentNumber = raw.optionalString("documentNumber") ?: raw.requiredString(MongoDocumentFields.ID),
            accessKey = raw.optionalString("accessKey"),
            authorizationNumber = raw.optionalString("authorizationNumber"),
            status = raw.optionalString("status") ?: "unknown",
            issuedAt = raw.instantOrNow("issuedAt"),
            authorizedAt = raw.instantOrNull("authorizedAt"),
            subtotal = totals.moneyOrZero("subtotal"),
            discountTotal = totals.moneyOrZero("discount"),
            taxTotal = totals.moneyOrZero("taxTotal"),
            grandTotal = totals.moneyOrZero("grandTotal"),
        )
    }

    private fun aggregateTopItems(saleDocs: List<Document>): List<AdminTopItemReportLine> {
        val accumulator = linkedMapOf<String, MutableTopItem>()
        saleDocs.filter { it.optionalString("operationalStatus") != "canceled" }.forEach { sale ->
            sale.documentList("items").filter { it.optionalString("status") != "canceled" }.forEach { item ->
                val catalogItemId = item.optionalString("catalogItemId")
                val name = item.optionalString("name") ?: item.optionalString("description") ?: "Item"
                val key = catalogItemId ?: name.lowercase()
                val current = accumulator.getOrPut(key) { MutableTopItem(catalogItemId, name) }
                current.quantity += item.optionalDocument("quantity")?.decimal("value") ?: BigDecimal.ONE
                current.netTotal = current.netTotal.safePlus(item.money("netTotal"))
                current.lineTotal = current.lineTotal.safePlus(item.money("lineTotal"))
            }
        }
        return accumulator.values.sortedByDescending { it.lineTotal.amount }.take(10).map {
            AdminTopItemReportLine(
                catalogItemId = it.catalogItemId,
                name = it.name,
                quantity = it.quantity,
                netTotal = it.netTotal,
                lineTotal = it.lineTotal,
            )
        }
    }

    private fun aggregateTaxLines(docs: List<Document>, fallbackCurrency: String): List<AdminTaxSummaryLine> {
        val accumulator = linkedMapOf<String, MutableTaxLine>()
        docs.forEach { document ->
            val taxes = document.optionalDocument("taxSnapshot")?.documentList("taxes").orEmpty()
            taxes.forEach { tax ->
                val taxCode = tax.optionalString("taxCode") ?: tax.optionalString("codigo") ?: "unknown"
                val rateCode = tax.optionalString("rateCode") ?: tax.optionalString("codigoPorcentaje") ?: "unknown"
                val key = "$taxCode::$rateCode"
                val current = accumulator.getOrPut(key) {
                    MutableTaxLine(
                        taxCode = taxCode,
                        rateCode = rateCode,
                        rate = tax.decimal("rate"),
                        taxableBase = AdminMoneyAmount.zero(fallbackCurrency),
                        taxAmount = AdminMoneyAmount.zero(fallbackCurrency),
                    )
                }
                current.taxableBase = current.taxableBase.safePlus(tax.money("taxableBase"))
                current.taxAmount = current.taxAmount.safePlus(tax.money("amount"))
                current.documentIds += document.requiredString(MongoDocumentFields.ID)
            }
        }
        return accumulator.values.sortedWith(compareBy({ it.taxCode }, { it.rateCode })).map {
            AdminTaxSummaryLine(
                taxCode = it.taxCode,
                rateCode = it.rateCode,
                rate = it.rate,
                taxableBase = it.taxableBase,
                taxAmount = it.taxAmount,
                documentCount = it.documentIds.size,
            )
        }
    }
}

private data class MutableTopItem(
    val catalogItemId: String?,
    val name: String,
    var quantity: BigDecimal = BigDecimal.ZERO,
    var netTotal: AdminMoneyAmount = AdminMoneyAmount.zero(),
    var lineTotal: AdminMoneyAmount = AdminMoneyAmount.zero(),
)

private data class MutableTaxLine(
    val taxCode: String,
    val rateCode: String,
    val rate: BigDecimal,
    var taxableBase: AdminMoneyAmount,
    var taxAmount: AdminMoneyAmount,
    val documentIds: MutableSet<String> = linkedSetOf(),
)

private fun String.takeIfNotBlank(): String? = trim().takeIf { it.isNotBlank() }

private fun Set<String>.normalizedStorageValues(): List<String> =
    map { it.trim().lowercase().replace('-', '_') }.filter { it.isNotBlank() }

private fun Instant.toDate(): Date = Date.from(this)

private fun Document.requiredString(field: String): String = getString(field)?.takeIf { it.isNotBlank() }
    ?: throw IllegalArgumentException("Required string field '$field' is missing or blank.")

private fun Document.optionalString(field: String): String? = getString(field)?.takeIf { it.isNotBlank() }

private fun Document.optionalDocument(field: String): Document? = this[field] as? Document

@Suppress("UNCHECKED_CAST")
private fun Document.documentList(field: String): List<Document> =
    (this[field] as? List<*>)?.filterIsInstance<Document>().orEmpty()

private fun Document.instantOrNull(field: String): Instant? = when (val raw = this[field]) {
    is Date -> raw.toInstant()
    is Instant -> raw
    is String -> runCatching { Instant.parse(raw) }.getOrNull()
    else -> null
}

private fun Document.instantOrNow(field: String): Instant = instantOrNull(field) ?: Instant.EPOCH

private fun Document.intOrNull(field: String): Int? = when (val raw = this[field]) {
    is Int -> raw
    is Long -> raw.toInt()
    is Number -> raw.toInt()
    else -> null
}

private fun Document.money(field: String): AdminMoneyAmount =
    optionalDocument(field)?.toMoney() ?: AdminMoneyAmount.zero()

private fun Document?.moneyOrZero(field: String): AdminMoneyAmount = this?.money(field) ?: AdminMoneyAmount.zero()

private fun Document.toMoney(): AdminMoneyAmount = AdminMoneyAmount(
    amount = decimal("amount"),
    currency = optionalString("currency") ?: "USD",
)

private fun Document.decimal(field: String): BigDecimal = when (val raw = this[field]) {
    is Decimal128 -> raw.bigDecimalValue()
    is BigDecimal -> raw
    is Int -> raw.toBigDecimal()
    is Long -> raw.toBigDecimal()
    is Double -> BigDecimal.valueOf(raw)
    is Number -> BigDecimal(raw.toString())
    is String -> runCatching { BigDecimal(raw) }.getOrDefault(BigDecimal.ZERO)
    else -> BigDecimal.ZERO
}

private fun AdminMoneyAmount.safePlus(other: AdminMoneyAmount): AdminMoneyAmount =
    if (currency == other.currency) plus(other) else this

private fun AdminMoneyAmount.safeMinus(other: AdminMoneyAmount): AdminMoneyAmount =
    if (currency == other.currency) minus(other) else this

private fun <T> List<T>.statusCounts(selector: (T) -> String): List<AdminStatusCount> =
    groupingBy(selector).eachCount().entries.sortedBy { it.key }
        .map { AdminStatusCount(status = it.key, count = it.value) }
