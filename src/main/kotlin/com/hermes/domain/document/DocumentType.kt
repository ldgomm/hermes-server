package com.hermes.domain.document

enum class DocumentType(val storageValue: String) {
    INTERNAL_TICKET("internal_ticket"),
    PHYSICAL_SALE_NOTE_REGISTRY("physical_sale_note_registry"),
    ELECTRONIC_INVOICE("electronic_invoice"),
    CREDIT_NOTE("credit_note"),
    DEBIT_NOTE("debit_note"),
    WITHHOLDING("withholding"),
    REMISSION_GUIDE("remission_guide");

    val isPhase10OperationalDocument: Boolean
        get() = this in setOf(INTERNAL_TICKET, PHYSICAL_SALE_NOTE_REGISTRY)

    companion object {
        fun fromStorage(value: String): DocumentType =
            entries.firstOrNull { it.storageValue == value.trim().lowercase() }
                ?: throw IllegalArgumentException("Unknown document type: $value")
    }
}
