package com.hermes.domain.tax

enum class TaxKind {
    IVA,
    ICE,
    IRBPNR,
    ISD,
    OTHER,
}

/**
 * Tax treatment must stay semantically explicit for Ecuador/SRI sales snapshots.
 *
 * Do not collapse IVA_FULL, IVA_ZERO, EXEMPT_IVA and NOT_SUBJECT_TO_IVA into a
 * generic boolean/taxable flag. Sale snapshots and future electronic documents
 * need to preserve the exact treatment used at sale time.
 */
enum class TaxTreatment {
    IVA_FULL,
    IVA_REDUCED_OR_SPECIAL,
    IVA_ZERO,
    EXEMPT_IVA,
    NOT_SUBJECT_TO_IVA,
    NO_TAX_INTERNAL,
}

enum class TaxRateStatus {
    DRAFT,
    ACTIVE,
    DEPRECATED,
    ARCHIVED,
}

enum class TaxProfileStatus {
    DRAFT,
    ACTIVE,
    DEPRECATED,
    ARCHIVED,
}

enum class TaxRegimeCode {
    RIMPE_POPULAR,
    RIMPE_ENTREPRENEUR,
    GENERAL,
    UNKNOWN,
    CUSTOM_VERIFIED,
}

enum class PriceTaxMode {
    TAX_EXCLUSIVE,
    TAX_INCLUSIVE,
}

enum class TaxSource {
    SYSTEM_SEED,
    PLATFORM_ADMIN,
    ORGANIZATION_ADMIN,
    MIGRATION,
    IMPORT,
}

enum class TaxEmissionType {
    INTERNAL_TICKET,
    PHYSICAL_SALE_NOTE_REGISTRY,
    ELECTRONIC_INVOICE,
}
