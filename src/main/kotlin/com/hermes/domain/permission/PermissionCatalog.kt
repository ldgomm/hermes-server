package com.hermes.domain.permission

object PermissionCatalog {
    const val ALL = "*"

    const val SALES_VIEW = "sales.view"
    const val SALES_CREATE = "sales.create"
    const val SALES_CANCEL = "sales.cancel"
    const val PAYMENTS_COLLECT = "payments.collect"
    const val CASH_OPEN = "cash.open"
    const val CASH_CLOSE = "cash.close"
    const val CATALOG_VIEW = "catalog.view"
    const val CATALOG_MANAGE_LOCAL = "catalog.manage_local"
    const val CATALOG_MANAGE_MASTER = "catalog.manage_master"
    const val TAX_MANAGE = "tax.manage"
    const val SIGNATURE_VIEW_METADATA = "signature.view_metadata"
    const val SIGNATURE_UPLOAD = "signature.upload"
    const val SIGNATURE_USE_FOR_INVOICING = "signature.use_for_invoicing"
    const val AUDIT_VIEW = "audit.view"

    val known: Set<String> = setOf(
        ALL,
        SALES_VIEW,
        SALES_CREATE,
        SALES_CANCEL,
        PAYMENTS_COLLECT,
        CASH_OPEN,
        CASH_CLOSE,
        CATALOG_VIEW,
        CATALOG_MANAGE_LOCAL,
        CATALOG_MANAGE_MASTER,
        TAX_MANAGE,
        SIGNATURE_VIEW_METADATA,
        SIGNATURE_UPLOAD,
        SIGNATURE_USE_FOR_INVOICING,
        AUDIT_VIEW,
    )
}
