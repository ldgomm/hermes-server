package com.hermes.domain.catalog

enum class CatalogTemplateStatus { DRAFT, ACTIVE, PAUSED, ARCHIVED }
enum class CatalogCategoryStatus { DRAFT, ACTIVE, PAUSED, ARCHIVED }
enum class CatalogItemStatus { DRAFT, ACTIVE, PAUSED, OUT_OF_STOCK, ARCHIVED, REMOVED_FROM_ACCOUNT }
enum class CatalogItemType { PRODUCT, SERVICE, PACKAGE, RENTAL, FEE }
enum class PublicDiscoveryStatus { PRIVATE, PUBLIC_PENDING_REVIEW, PUBLIC, HIDDEN_TEMPORARILY, SUSPENDED_BY_PLATFORM, ARCHIVED }

enum class CatalogIdentifierType {
    SKU_MASTER,
    SKU_LOCAL,
    INTERNAL_CODE,
    SUPPLIER_CODE,
    BARCODE,
    GTIN,
    EAN_8,
    EAN_13,
    UPC_A,
    ISBN,
    MANUFACTURER_PART_NUMBER
}

enum class CatalogIdentifierScope { GLOBAL, ORGANIZATION, BRANCH }
enum class CatalogIdentifierStatus { PROPOSED, ACTIVE, VERIFIED, CONFLICT, DEPRECATED, REJECTED }
enum class CatalogIdentifierSource { PLATFORM, ORGANIZATION, SUPPLIER, IMPORT }

enum class CatalogAttributeType { TEXT, DECIMAL, INTEGER, BOOLEAN, ENUM }
enum class CatalogMediaOwnerKind { MASTER, LOCAL }
enum class CatalogMediaStatus { PENDING, APPROVED, REJECTED, HIDDEN }
enum class CatalogImportJobStatus { UPLOADED, MAPPING_REQUIRED, VALIDATING, MATCHED, NEEDS_REVIEW, READY_TO_COMMIT, COMMITTED, FAILED, CANCELED }

enum class ReturnPolicyType { FINAL_SALE, EXCHANGE_ONLY, REFUND_ALLOWED, WARRANTY_ONLY }
enum class WarrantyPolicyType { NONE, DAYS, MONTHS, MANUFACTURER }
