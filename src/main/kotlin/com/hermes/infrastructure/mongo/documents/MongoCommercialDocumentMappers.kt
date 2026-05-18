package com.hermes.infrastructure.mongo.documents

import com.hermes.domain.document.*
import com.hermes.domain.money.CurrencyCode
import com.hermes.domain.money.Money
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.sale.TaxProfileSnapshotForSale
import com.hermes.domain.tax.TaxTreatment
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoDecimalMapper
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import org.bson.Document
import java.time.LocalDate

internal object MongoCommercialDocumentMappers {
    fun toDocument(document: CommercialDocument): Document = Document(MongoDocumentFields.ID, document.id)
        .append(MongoDocumentFields.ORGANIZATION_ID, document.organizationId)
        .append("branchId", document.branchId)
        .append("emissionPointId", document.emissionPointId)
        .append("saleId", document.saleId)
        .append("customerId", document.customerId)
        .append("documentType", document.documentType.storageValue)
        .append("documentNumber", document.documentNumber)
        .append("accessKey", document.accessKey)
        .append("authorizationNumber", document.authorizationNumber)
        .append("status", document.status.name.lowercase())
        .append("issuedAt", MongoInstantMapper.toDate(document.issuedAt))
        .append("authorizedAt", document.authorizedAt?.let(MongoInstantMapper::toDate))
        .append("totalsSnapshot", totalsToDocument(document.totalsSnapshot))
        .append("taxSnapshot", taxSnapshotToDocument(document.taxSnapshot))
        .append("lineSnapshots", document.lineSnapshots.map(::lineToDocument))
        .append("payloadId", document.payloadId)
        .append("pdfObjectKey", document.pdfObjectKey)
        .append("emailedAt", document.emailedAt?.let(MongoInstantMapper::toDate))
        .append("emailTo", document.emailTo)
        .append("notes", document.notes)
        .append(MongoDocumentFields.CREATED_AT, MongoInstantMapper.toDate(document.createdAt))
        .append(MongoDocumentFields.CREATED_BY, document.createdBy)
        .append(MongoDocumentFields.UPDATED_AT, MongoInstantMapper.toDate(document.updatedAt))
        .append(MongoDocumentFields.UPDATED_BY, document.updatedBy)
        .append(MongoDocumentFields.VERSION, document.version.toInt())
        .append(MongoDocumentFields.SCHEMA_VERSION, 1)

    fun fromDocument(raw: Document): CommercialDocument = CommercialDocument(
        id = raw.requiredString(MongoDocumentFields.ID),
        organizationId = raw.requiredString(MongoDocumentFields.ORGANIZATION_ID),
        branchId = raw.requiredString("branchId"),
        emissionPointId = raw.optionalString("emissionPointId"),
        saleId = raw.optionalString("saleId"),
        customerId = raw.optionalString("customerId"),
        documentType = DocumentType.fromStorage(raw.requiredString("documentType")),
        documentNumber = raw.requiredString("documentNumber"),
        accessKey = raw.optionalString("accessKey"),
        authorizationNumber = raw.optionalString("authorizationNumber"),
        status = enumValueOf<DocumentStatus>(raw.requiredString("status").uppercase()),
        issuedAt = MongoInstantMapper.readRequired(raw, "issuedAt"),
        authorizedAt = MongoInstantMapper.readOptional(raw, "authorizedAt"),
        totalsSnapshot = totalsFromDocument(raw.requiredDocument("totalsSnapshot")),
        taxSnapshot = taxSnapshotFromDocument(raw.requiredDocument("taxSnapshot")),
        lineSnapshots = raw.documentList("lineSnapshots").map(::lineFromDocument),
        payloadId = raw.optionalString("payloadId"),
        pdfObjectKey = raw.optionalString("pdfObjectKey"),
        emailedAt = MongoInstantMapper.readOptional(raw, "emailedAt"),
        emailTo = raw.optionalString("emailTo"),
        notes = raw.optionalString("notes"),
        createdAt = MongoInstantMapper.readRequired(raw, MongoDocumentFields.CREATED_AT),
        createdBy = raw.optionalString(MongoDocumentFields.CREATED_BY),
        updatedAt = MongoInstantMapper.readRequired(raw, MongoDocumentFields.UPDATED_AT),
        updatedBy = raw.optionalString(MongoDocumentFields.UPDATED_BY),
        version = raw.readLong(MongoDocumentFields.VERSION),
    )

    private fun totalsToDocument(snapshot: CommercialDocumentTotalsSnapshot): Document = Document()
        .append("subtotal", moneyToDocument(snapshot.subtotal))
        .append("discount", moneyToDocument(snapshot.discount))
        .append("taxTotal", moneyToDocument(snapshot.taxTotal))
        .append("grandTotal", moneyToDocument(snapshot.grandTotal))
        .append("paidAmount", moneyToDocument(snapshot.paidAmount))
        .append("currency", snapshot.currency.value)
        .append("paymentStatus", snapshot.paymentStatus)

    private fun totalsFromDocument(raw: Document): CommercialDocumentTotalsSnapshot = CommercialDocumentTotalsSnapshot(
        subtotal = moneyFromDocument(raw.requiredDocument("subtotal")),
        discount = moneyFromDocument(raw.requiredDocument("discount")),
        taxTotal = moneyFromDocument(raw.requiredDocument("taxTotal")),
        grandTotal = moneyFromDocument(raw.requiredDocument("grandTotal")),
        paidAmount = raw.optionalDocument("paidAmount")?.let(::moneyFromDocument)
            ?: Money.zero(CurrencyCode(raw.requiredString("currency"))),
        currency = CurrencyCode(raw.requiredString("currency")),
        paymentStatus = raw.optionalString("paymentStatus") ?: "UNKNOWN",
    )

    private fun taxSnapshotToDocument(snapshot: CommercialDocumentTaxSnapshot): Document = Document()
        .append("taxTotal", moneyToDocument(snapshot.taxTotal))
        .append("taxes", snapshot.taxes.map(::taxLineToDocument))

    private fun taxSnapshotFromDocument(raw: Document): CommercialDocumentTaxSnapshot = CommercialDocumentTaxSnapshot(
        taxTotal = moneyFromDocument(raw.requiredDocument("taxTotal")),
        taxes = raw.documentList("taxes").map(::taxLineFromDocument),
    )

    private fun taxLineToDocument(tax: CommercialDocumentTaxLineSnapshot): Document = Document()
        .append("taxCode", tax.taxCode)
        .append("rateCode", tax.rateCode)
        .append("rate", MongoDecimalMapper.percentageToDecimal128(tax.rate.value))
        .append("taxableBase", moneyToDocument(tax.taxableBase))
        .append("amount", moneyToDocument(tax.amount))

    private fun taxLineFromDocument(raw: Document): CommercialDocumentTaxLineSnapshot =
        CommercialDocumentTaxLineSnapshot(
            taxCode = raw.requiredString("taxCode"),
            rateCode = raw.requiredString("rateCode"),
            rate = Percentage.of(MongoDecimalMapper.readRequired(raw, "rate")),
            taxableBase = moneyFromDocument(raw.requiredDocument("taxableBase")),
            amount = moneyFromDocument(raw.requiredDocument("amount")),
        )

    private fun lineToDocument(line: CommercialDocumentLineSnapshot): Document = Document()
        .append("saleItemId", line.saleItemId)
        .append("catalogItemId", line.catalogItemId)
        .append("description", line.description)
        .append("quantity", quantityToDocument(line.quantity))
        .append("unitPrice", moneyToDocument(line.unitPrice))
        .append("discount", moneyToDocument(line.discount))
        .append("netTotal", moneyToDocument(line.netTotal))
        .append("taxTotal", moneyToDocument(line.taxTotal))
        .append("lineTotal", moneyToDocument(line.lineTotal))
        .append("taxProfileSnapshot", taxProfileToDocument(line.taxProfileSnapshot))

    private fun lineFromDocument(raw: Document): CommercialDocumentLineSnapshot = CommercialDocumentLineSnapshot(
        saleItemId = raw.requiredString("saleItemId"),
        catalogItemId = raw.requiredString("catalogItemId"),
        description = raw.requiredString("description"),
        quantity = quantityFromDocument(raw.requiredDocument("quantity")),
        unitPrice = moneyFromDocument(raw.requiredDocument("unitPrice")),
        discount = moneyFromDocument(raw.requiredDocument("discount")),
        netTotal = moneyFromDocument(raw.requiredDocument("netTotal")),
        taxTotal = moneyFromDocument(raw.requiredDocument("taxTotal")),
        lineTotal = moneyFromDocument(raw.requiredDocument("lineTotal")),
        taxProfileSnapshot = taxProfileFromDocument(raw.requiredDocument("taxProfileSnapshot")),
    )

    private fun taxProfileToDocument(snapshot: TaxProfileSnapshotForSale): Document = Document()
        .append("code", snapshot.code)
        .append("taxName", snapshot.taxName)
        .append("rate", MongoDecimalMapper.percentageToDecimal128(snapshot.rate.value))
        .append("sriTaxCode", snapshot.sriTaxCode)
        .append("sriRateCode", snapshot.sriRateCode)
        .append("treatment", snapshot.treatment.name)
        .append("legalBasis", snapshot.legalBasis)
        .append("effectiveFrom", snapshot.effectiveFrom.toString())
        .append("source", snapshot.source)

    private fun taxProfileFromDocument(raw: Document): TaxProfileSnapshotForSale = TaxProfileSnapshotForSale(
        code = raw.requiredString("code"),
        taxName = raw.requiredString("taxName"),
        rate = Percentage.of(MongoDecimalMapper.readRequired(raw, "rate")),
        sriTaxCode = raw.requiredString("sriTaxCode"),
        sriRateCode = raw.requiredString("sriRateCode"),
        treatment = enumValueOf<TaxTreatment>(raw.requiredString("treatment")),
        legalBasis = raw.requiredString("legalBasis"),
        effectiveFrom = LocalDate.parse(raw.requiredString("effectiveFrom")),
        source = raw.requiredString("source"),
    )

    private fun moneyToDocument(money: Money): Document = Document()
        .append("amount", MongoDecimalMapper.moneyToDecimal128(money.amount))
        .append("currency", money.currency.value)

    private fun moneyFromDocument(raw: Document): Money = Money.of(
        amount = MongoDecimalMapper.readRequired(raw, "amount"),
        currency = CurrencyCode(raw.requiredString("currency")),
    )

    private fun quantityToDocument(quantity: Quantity): Document = Document()
        .append("value", MongoDecimalMapper.quantityToDecimal128(quantity.value))
        .append("unitCode", quantity.unitCode)
        .append("allowsDecimal", quantity.allowsDecimal)

    private fun quantityFromDocument(raw: Document): Quantity = Quantity.of(
        value = MongoDecimalMapper.readRequired(raw, "value"),
        unitCode = raw.requiredString("unitCode"),
        allowsDecimal = raw.getBoolean("allowsDecimal", false),
    )
}

private fun Document.requiredString(field: String): String =
    getString(field)?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Required string field '$field' is missing or blank.")

private fun Document.optionalString(field: String): String? =
    getString(field)?.takeIf { it.isNotBlank() }

private fun Document.optionalDocument(field: String): Document? = this[field] as? Document

private fun Document.requiredDocument(field: String): Document =
    optionalDocument(field) ?: throw IllegalArgumentException("Required document field '$field' is missing.")

@Suppress("UNCHECKED_CAST")
private fun Document.documentList(field: String): List<Document> =
    (this[field] as? List<*>)?.filterIsInstance<Document>().orEmpty()

private fun Document.readLong(field: String): Long = when (val raw = this[field]) {
    is Int -> raw.toLong()
    is Long -> raw
    is Number -> raw.toLong()
    else -> 1L
}
