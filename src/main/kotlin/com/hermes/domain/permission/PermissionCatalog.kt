package com.hermes.domain.permission

object PermissionCatalog {
    const val ALL = "*"

    // Admin General / Business Foundation.
    const val ADMIN_BUSINESS_VIEW = "admin.business.view"
    const val ADMIN_BUSINESS_UPDATE = "admin.business.update"
    const val ADMIN_BUSINESS_READINESS_VIEW = "admin.business.readiness.view"

    const val ADMIN_ACTIVITIES_VIEW = "admin.activities.view"
    const val ADMIN_ACTIVITIES_MANAGE = "admin.activities.manage"

    const val ADMIN_BRANCHES_VIEW = "admin.branches.view"
    const val ADMIN_BRANCHES_MANAGE = "admin.branches.manage"

    const val ADMIN_EMISSION_POINTS_VIEW = "admin.emission_points.view"
    const val ADMIN_EMISSION_POINTS_MANAGE = "admin.emission_points.manage"

    // Existing constants kept for backward compatibility with Fase 3/4 code and tests.
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

    // Credentials / users.
    const val CREDENTIALS_USERS_VIEW = "credentials.users.view"
    const val CREDENTIALS_USERS_CREATE = "credentials.users.create"
    const val CREDENTIALS_USERS_INVITE = "credentials.users.invite"
    const val CREDENTIALS_USERS_RESET_PASSWORD = "credentials.users.reset_password"
    const val CREDENTIALS_USERS_BLOCK = "credentials.users.block"
    const val CREDENTIALS_USERS_UNBLOCK = "credentials.users.unblock"
    const val CREDENTIALS_SESSIONS_REVOKE = "credentials.sessions.revoke"
    const val CREDENTIALS_ROLES_VIEW = "credentials.roles.view"
    const val CREDENTIALS_ROLES_ASSIGN = "credentials.roles.assign"
    const val CREDENTIALS_ROLES_MANAGE = "credentials.roles.manage"

    // Organization / settings.
    const val ORGANIZATION_VIEW = "organization.view"
    const val ORGANIZATION_UPDATE = "organization.update"
    const val ORGANIZATION_UPDATE_PROFILE = "organization.update_profile"
    const val ORGANIZATION_MEMBERS_VIEW = "organization.members.view"
    const val ORGANIZATION_MEMBERS_MANAGE = "organization.members.manage"

    const val BRANCHES_VIEW = "branches.view"
    const val BRANCHES_CREATE = "branches.create"
    const val BRANCHES_UPDATE = "branches.update"
    const val SETTINGS_BRANCHES_VIEW = "settings.branches.view"
    const val SETTINGS_BRANCHES_MANAGE = "settings.branches.manage"
    const val SETTINGS_EMISSION_POINTS_VIEW = "settings.emission_points.view"
    const val SETTINGS_EMISSION_POINTS_MANAGE = "settings.emission_points.manage"

    // Activities.
    const val ACTIVITIES_VIEW = "activities.view"
    const val ACTIVITIES_CREATE = "activities.create"
    const val ACTIVITIES_UPDATE = "activities.update"

    // Catalog.
    const val CATALOG_LOCAL_VIEW = "catalog.local.view"
    const val CATALOG_LOCAL_COPY_FROM_MASTER = "catalog.local.copy_from_master"
    const val CATALOG_LOCAL_UPDATE_LOCAL_COPY = "catalog.local.update_local_copy"
    const val CATALOG_LOCAL_CHANGE_PRICE = "catalog.local.change_price"
    const val CATALOG_LOCAL_CHANGE_TAX_PROFILE = "catalog.local.change_tax_profile"
    const val CATALOG_LOCAL_DISABLE_LOCAL_COPY = "catalog.local.disable_local_copy"
    const val CATALOG_LOCAL_REQUEST_NEW_ITEM = "catalog.local.request_new_item"
    const val CATALOG_IDENTIFIERS_SCAN = "catalog.identifiers.scan"
    const val CATALOG_IDENTIFIERS_ADD_LOCAL = "catalog.identifiers.add_local"
    const val CATALOG_IDENTIFIERS_VERIFY_MASTER = "catalog.identifiers.verify_master"
    const val CATALOG_IDENTIFIERS_RESOLVE_CONFLICT = "catalog.identifiers.resolve_conflict"
    const val CATALOG_IMPORT_COMMIT = "catalog.import.commit"
    const val CATALOG_UNITS_MANAGE_CONVERSIONS = "catalog.units.manage_conversions"

    // Customers.
    const val CUSTOMERS_VIEW = "customers.view"
    const val CUSTOMERS_CREATE = "customers.create"
    const val CUSTOMERS_UPDATE = "customers.update"

    // Sales.
    const val SALES_CONFIRM = "sales.confirm"
    const val SALES_CANCEL_AFTER_PAYMENT = "sales.cancel_after_payment"
    const val SALES_CLOSE = "sales.close"
    const val SALES_APPLY_DISCOUNT = "sales.apply_discount"
    const val SALES_ITEMS_CHANGE_STATUS = "sales.items.change_status"

    // Payments / receivables.
    const val PAYMENTS_VIEW = "payments.view"
    const val PAYMENTS_PARTIAL_COLLECT = "payments.partial_collect"
    const val PAYMENTS_MARK_AS_CREDIT = "payments.mark_as_credit"
    const val PAYMENTS_REFUND = "payments.refund"
    const val PAYMENTS_REVERSE = "payments.reverse"

    const val RECEIVABLES_VIEW = "receivables.view"
    const val RECEIVABLES_CREATE = "receivables.create"
    const val RECEIVABLES_REGISTER_PAYMENT = "receivables.register_payment"

    // Cash.
    const val CASH_VIEW = "cash.view"
    const val CASH_SESSION_VIEW_CURRENT = "cash.session.view_current"
    const val CASH_SESSION_VIEW_HISTORY = "cash.session.view_history"
    const val CASH_SESSION_OPEN = "cash.session.open"
    const val CASH_SESSION_CLOSE = "cash.session.close"
    const val CASH_MOVEMENTS_REGISTER_INFLOW = "cash.movements.register_inflow"
    const val CASH_MOVEMENTS_REGISTER_OUTFLOW = "cash.movements.register_outflow"
    const val CASH_MOVEMENTS_ADJUST = "cash.movements.adjust"

    // Inventory.
    const val INVENTORY_VIEW = "inventory.view"
    const val INVENTORY_ADJUST = "inventory.adjust"

    // Documents.
    const val DOCUMENTS_VIEW = "documents.view"
    const val DOCUMENTS_GENERATE_INTERNAL_TICKET = "documents.generate_internal_ticket"
    const val DOCUMENTS_GENERATE_PHYSICAL_SALE_NOTE_REGISTRY = "documents.generate_physical_sale_note_registry"
    const val DOCUMENTS_ISSUE_ELECTRONIC_INVOICE = "documents.issue_electronic_invoice"
    const val DOCUMENTS_DOWNLOAD_PDF = "documents.download_pdf"
    const val DOCUMENTS_DOWNLOAD_XML = "documents.download_xml"
    const val DOCUMENTS_INVOICE_ISSUE = "documents.invoice.issue"
    const val DOCUMENTS_INVOICE_DOWNLOAD_XML = "documents.invoice.download_xml"
    const val DOCUMENTS_INVOICE_DOWNLOAD_RIDE = "documents.invoice.download_ride"
    const val DOCUMENTS_INVOICE_REQUEST_CANCELLATION = "documents.invoice.request_cancellation"
    const val DOCUMENTS_ELECTRONIC_INVOICE_VIEW = "documents.electronic_invoice.view"
    const val DOCUMENTS_ELECTRONIC_INVOICE_LIST = "documents.electronic_invoice.list"
    const val DOCUMENTS_ELECTRONIC_INVOICE_ISSUE = "documents.electronic_invoice.issue"
    const val DOCUMENTS_ELECTRONIC_INVOICE_RETRY = "documents.electronic_invoice.retry"
    const val DOCUMENTS_ELECTRONIC_INVOICE_DOWNLOAD_XML = "documents.electronic_invoice.download_xml"
    const val DOCUMENTS_ELECTRONIC_INVOICE_DOWNLOAD_RIDE = "documents.electronic_invoice.download_ride"
    const val DOCUMENTS_ELECTRONIC_INVOICE_EMAIL = "documents.electronic_invoice.email"
    const val DOCUMENTS_ELECTRONIC_INVOICE_VIEW_ERRORS = "documents.electronic_invoice.view_errors"
    const val DOCUMENTS_ELECTRONIC_INVOICE_VIEW_AUDIT = "documents.electronic_invoice.view_audit"
    const val DOCUMENTS_ELECTRONIC_INVOICE_HOMOLOGATE = "documents.electronic_invoice.homologate"
    const val DOCUMENTS_ELECTRONIC_INVOICE_ENABLE_PRODUCTION = "documents.electronic_invoice.enable_production"
    const val DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS = "documents.electronic_invoice.manage_settings"

    // Tax.
    const val TAX_SETTINGS_VIEW = "tax.settings.view"
    const val TAX_SETTINGS_UPDATE_ORGANIZATION_REGIME = "tax.settings.update_organization_regime"
    const val TAX_PROFILES_ASSIGN_TO_ITEM = "tax.profiles.assign_to_item"

    // Signature.
    const val SIGNATURE_REPLACE = "signature.replace"
    const val SIGNATURE_REVOKE = "signature.revoke"
    const val SIGNATURE_TEST = "signature.test"
    const val SIGNATURE_VIEW_AUDIT = "signature.view_audit"

    // Reports.
    const val REPORTS_DASHBOARD_VIEW = "reports.dashboard.view"
    const val REPORTS_SALES_VIEW = "reports.sales.view"
    const val REPORTS_CASH_VIEW = "reports.cash.view"
    const val REPORTS_TAX_VIEW = "reports.tax.view"
    const val REPORTS_DOCUMENTS_VIEW = "reports.documents.view"

    // Future public discovery foundation.
    const val CATALOG_PRICE_VIEW = "catalog.price.view"
    const val CATALOG_PRICE_UPDATE = "catalog.price.update"
    const val CATALOG_PRICE_HISTORY_VIEW = "catalog.price_history.view"
    const val CATALOG_SEARCH_METADATA_VIEW = "catalog.search_metadata.view"
    const val CATALOG_SEARCH_METADATA_UPDATE = "catalog.search_metadata.update"
    const val CATALOG_PUBLIC_VISIBILITY_VIEW = "catalog.public_visibility.view"
    const val CATALOG_PUBLIC_VISIBILITY_UPDATE = "catalog.public_visibility.update"
    const val CATALOG_PUBLIC_VISIBILITY_HIDE = "catalog.public_visibility.hide"
    const val CATALOG_PUBLIC_VISIBILITY_REPUBLISH = "catalog.public_visibility.republish"
    const val CATALOG_PUBLIC_DISPLAY_UPDATE_PRICE_MODE = "catalog.public_display.update_price_mode"
    const val CATALOG_PUBLIC_DISPLAY_UPDATE_STOCK_MODE = "catalog.public_display.update_stock_mode"
    const val CATALOG_PUBLIC_DISPLAY_ALLOW_COMPARISON = "catalog.public_display.allow_comparison"
    const val CATALOG_PUBLIC_PREVIEW_VIEW = "catalog.public_preview.view"
    const val BRANCH_LOCATION_VIEW = "branch.location.view"
    const val BRANCH_LOCATION_UPDATE = "branch.location.update"
    const val BRANCH_LOCATION_PUBLIC_VISIBILITY_VIEW = "branch.location.public_visibility.view"
    const val BRANCH_LOCATION_PUBLIC_VISIBILITY_UPDATE = "branch.location.public_visibility.update"
    const val BRANCH_LOCATION_PRIVACY_MODE_UPDATE = "branch.location.privacy_mode.update"
    const val DISCOVERY_SETTINGS_VIEW = "discovery.settings.view"
    const val DISCOVERY_SETTINGS_UPDATE = "discovery.settings.update"
    const val DISCOVERY_PREVIEW_VIEW = "discovery.preview.view"
    const val DISCOVERY_AUDIT_VIEW = "discovery.audit.view"
    const val BUSINESS_HOURS_VIEW = "business_hours.view"
    const val BUSINESS_HOURS_UPDATE = "business_hours.update"
    const val BUSINESS_HOURS_PUBLIC_VISIBILITY_UPDATE = "business_hours.public_visibility.update"

    // Future assisted commerce permissions. Seeded as reserved, not enabled in MVP.
    const val CONSUMER_REQUEST_LIST_VIEW = "consumer.request_list.view"
    const val CONSUMER_REQUEST_LIST_CREATE = "consumer.request_list.create"
    const val CONSUMER_SEARCH_CREATE = "consumer.search.create"
    const val CONSUMER_STORE_REQUEST_CREATE = "consumer.store_request.create"
    const val STORE_REQUESTS_VIEW = "store_requests.view"
    const val STORE_REQUESTS_ACCEPT = "store_requests.accept"
    const val STORE_REQUESTS_REJECT = "store_requests.reject"
    const val STORE_REQUESTS_CLOSE = "store_requests.close"
    const val STORE_REQUESTS_QUOTES_CREATE = "store_requests.quotes.create"
    const val STORE_REQUESTS_CONVERT_TO_SALE = "store_requests.convert_to_sale"
    const val STORE_REQUESTS_CONVERT_TO_RESERVATION = "store_requests.convert_to_reservation"
    const val CHAT_CONVERSATIONS_VIEW = "chat.conversations.view"
    const val CHAT_MESSAGES_SEND = "chat.messages.send"
    const val CHAT_ATTACHMENTS_UPLOAD = "chat.attachments.upload"
    const val PLATFORM_ASSISTED_COMMERCE_MODERATE = "platform.assisted_commerce.moderate"

    val definitions: List<PermissionDefinition> = listOf(
        // Credentials / users.
        active(
            CREDENTIALS_USERS_VIEW,
            "View users",
            "View organization users.",
            PermissionCategory.CREDENTIALS,
            PermissionRiskLevel.MEDIUM
        ),
        critical(
            CREDENTIALS_USERS_CREATE,
            "Create users",
            "Create users for an organization.",
            PermissionCategory.CREDENTIALS
        ),
        critical(
            CREDENTIALS_USERS_INVITE, "Invite users", "Invite users to an organization.", PermissionCategory.CREDENTIALS
        ),
        critical(
            CREDENTIALS_USERS_RESET_PASSWORD,
            "Reset user password",
            "Start or complete a user password reset flow.",
            PermissionCategory.CREDENTIALS,
            requiresReason = true,
            requiresStepUp = true
        ),
        critical(
            CREDENTIALS_USERS_BLOCK,
            "Block users",
            "Block an organization user.",
            PermissionCategory.CREDENTIALS,
            requiresReason = true,
            requiresStepUp = true
        ),
        critical(
            CREDENTIALS_USERS_UNBLOCK,
            "Unblock users",
            "Unblock an organization user.",
            PermissionCategory.CREDENTIALS,
            requiresReason = true,
            requiresStepUp = true
        ),
        critical(
            CREDENTIALS_SESSIONS_REVOKE,
            "Revoke sessions",
            "Revoke one or many user sessions.",
            PermissionCategory.CREDENTIALS,
            requiresReason = true
        ),
        active(
            CREDENTIALS_ROLES_VIEW,
            "View roles",
            "View system and organization roles.",
            PermissionCategory.CREDENTIALS,
            PermissionRiskLevel.MEDIUM
        ),
        critical(
            CREDENTIALS_ROLES_ASSIGN,
            "Assign roles",
            "Assign or remove roles for organization members.",
            PermissionCategory.CREDENTIALS,
            requiresReason = true,
            requiresStepUp = true
        ),
        critical(
            CREDENTIALS_ROLES_MANAGE,
            "Manage roles",
            "Create or update custom organization roles.",
            PermissionCategory.CREDENTIALS,
            requiresReason = true,
            requiresStepUp = true
        ),

        // Organization / settings.
        active(
            ORGANIZATION_VIEW,
            "View organization",
            "View organization information.",
            PermissionCategory.ORGANIZATION,
            PermissionRiskLevel.LOW
        ),
        critical(
            ORGANIZATION_UPDATE,
            "Update organization",
            "Update legal or operational organization information.",
            PermissionCategory.ORGANIZATION,
            requiresAudit = true
        ),
        critical(
            ORGANIZATION_UPDATE_PROFILE,
            "Update organization profile",
            "Update public or internal organization profile.",
            PermissionCategory.ORGANIZATION,
            requiresAudit = true
        ),
        active(
            ORGANIZATION_MEMBERS_VIEW,
            "View organization members",
            "View organization memberships.",
            PermissionCategory.ORGANIZATION,
            PermissionRiskLevel.MEDIUM
        ),
        critical(
            ORGANIZATION_MEMBERS_MANAGE,
            "Manage organization members",
            "Create, suspend, remove or update memberships.",
            PermissionCategory.ORGANIZATION,
            requiresReason = true
        ),
        active(BRANCHES_VIEW, "View branches", "View branches.", PermissionCategory.SETTINGS, PermissionRiskLevel.LOW),
        active(
            BRANCHES_CREATE,
            "Create branches",
            "Create branches.",
            PermissionCategory.SETTINGS,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        active(
            BRANCHES_UPDATE,
            "Update branches",
            "Update branch information.",
            PermissionCategory.SETTINGS,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        active(
            SETTINGS_BRANCHES_VIEW,
            "View branch settings",
            "View branch settings.",
            PermissionCategory.SETTINGS,
            PermissionRiskLevel.LOW
        ),
        active(
            SETTINGS_BRANCHES_MANAGE,
            "Manage branch settings",
            "Manage branch settings.",
            PermissionCategory.SETTINGS,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        active(
            SETTINGS_EMISSION_POINTS_VIEW,
            "View emission points",
            "View document emission points.",
            PermissionCategory.SETTINGS,
            PermissionRiskLevel.MEDIUM
        ),
        critical(
            SETTINGS_EMISSION_POINTS_MANAGE,
            "Manage emission points",
            "Manage document emission points and related settings.",
            PermissionCategory.SETTINGS,
            requiresReason = true,
            requiresStepUp = true
        ),

        // Activities.
        active(
            ACTIVITIES_VIEW,
            "View activities",
            "View organization activities.",
            PermissionCategory.ACTIVITIES,
            PermissionRiskLevel.LOW
        ),
        active(
            ACTIVITIES_CREATE,
            "Create activities",
            "Create organization activities.",
            PermissionCategory.ACTIVITIES,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        active(
            ACTIVITIES_UPDATE,
            "Update activities",
            "Update organization activities.",
            PermissionCategory.ACTIVITIES,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),

        // Catalog.
        active(
            CATALOG_VIEW, "View catalog", "View catalog items.", PermissionCategory.CATALOG, PermissionRiskLevel.LOW
        ),
        active(
            CATALOG_LOCAL_VIEW,
            "View local catalog",
            "View organization local catalog.",
            PermissionCategory.CATALOG,
            PermissionRiskLevel.LOW
        ),
        active(
            CATALOG_LOCAL_COPY_FROM_MASTER,
            "Copy catalog item",
            "Copy a master catalog template into an organization.",
            PermissionCategory.CATALOG,
            PermissionRiskLevel.MEDIUM,
            requiresAudit = true
        ),
        active(
            CATALOG_LOCAL_UPDATE_LOCAL_COPY,
            "Update local catalog copy",
            "Update local editable fields.",
            PermissionCategory.CATALOG,
            PermissionRiskLevel.MEDIUM,
            requiresAudit = true
        ),
        active(
            CATALOG_LOCAL_CHANGE_PRICE,
            "Change local price",
            "Change local item price.",
            PermissionCategory.CATALOG,
            PermissionRiskLevel.HIGH,
            requiresAudit = true,
            requiresReason = true
        ),
        active(
            CATALOG_LOCAL_CHANGE_TAX_PROFILE,
            "Change item tax profile",
            "Change tax profile assigned to an item.",
            PermissionCategory.CATALOG,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        active(
            CATALOG_LOCAL_DISABLE_LOCAL_COPY,
            "Disable local catalog copy",
            "Disable or pause a local catalog item.",
            PermissionCategory.CATALOG,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        active(
            CATALOG_LOCAL_REQUEST_NEW_ITEM,
            "Request catalog item",
            "Request a new platform catalog item.",
            PermissionCategory.CATALOG,
            PermissionRiskLevel.LOW
        ),
        active(
            CATALOG_IDENTIFIERS_SCAN,
            "Scan catalog identifiers",
            "Search or scan SKU, barcode or internal item identifiers.",
            PermissionCategory.CATALOG,
            PermissionRiskLevel.LOW
        ),
        active(
            CATALOG_IDENTIFIERS_ADD_LOCAL,
            "Add local identifier",
            "Associate a local identifier with a catalog item.",
            PermissionCategory.CATALOG,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        active(
            CATALOG_UNITS_MANAGE_CONVERSIONS,
            "Manage unit conversions",
            "Manage unit and conversion rules.",
            PermissionCategory.CATALOG,
            PermissionRiskLevel.HIGH,
            requiresAudit = true,
            requiresReason = true
        ),
        platform(
            CATALOG_MANAGE_MASTER,
            "Manage master catalog",
            "Manage platform master catalog.",
            PermissionCategory.CATALOG,
            PermissionRiskLevel.CRITICAL
        ),
        platform(
            CATALOG_IDENTIFIERS_VERIFY_MASTER,
            "Verify master identifier",
            "Verify a master catalog identifier.",
            PermissionCategory.CATALOG,
            PermissionRiskLevel.CRITICAL
        ),
        platform(
            CATALOG_IDENTIFIERS_RESOLVE_CONFLICT,
            "Resolve identifier conflict",
            "Resolve global catalog identifier conflicts.",
            PermissionCategory.CATALOG,
            PermissionRiskLevel.CRITICAL
        ),
        active(
            CATALOG_IMPORT_COMMIT,
            "Commit catalog import",
            "Commit a reviewed catalog import job.",
            PermissionCategory.CATALOG,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),

        // Customers.
        active(
            CUSTOMERS_VIEW, "View customers", "View customers.", PermissionCategory.CUSTOMERS, PermissionRiskLevel.LOW
        ),
        active(
            CUSTOMERS_CREATE,
            "Create customers",
            "Create customers.",
            PermissionCategory.CUSTOMERS,
            PermissionRiskLevel.MEDIUM
        ),
        active(
            CUSTOMERS_UPDATE,
            "Update customers",
            "Update customer data.",
            PermissionCategory.CUSTOMERS,
            PermissionRiskLevel.MEDIUM,
            requiresAudit = true
        ),

        // Sales.
        active(SALES_VIEW, "View sales", "View sales.", PermissionCategory.SALES, PermissionRiskLevel.LOW),
        active(SALES_CREATE, "Create sales", "Create sales.", PermissionCategory.SALES, PermissionRiskLevel.MEDIUM),
        active(
            SALES_CONFIRM,
            "Confirm sales",
            "Confirm pending sales.",
            PermissionCategory.SALES,
            PermissionRiskLevel.MEDIUM,
            requiresAudit = true
        ),
        active(
            SALES_CANCEL,
            "Cancel sales",
            "Cancel sales before closing.",
            PermissionCategory.SALES,
            PermissionRiskLevel.HIGH,
            requiresAudit = true,
            requiresReason = true
        ),
        active(
            SALES_CANCEL_AFTER_PAYMENT,
            "Cancel paid sale",
            "Cancel a sale after a payment exists.",
            PermissionCategory.SALES,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        active(
            SALES_CLOSE,
            "Close sales",
            "Close delivered or completed sales.",
            PermissionCategory.SALES,
            PermissionRiskLevel.MEDIUM,
            requiresAudit = true
        ),
        active(
            SALES_APPLY_DISCOUNT,
            "Apply sale discount",
            "Apply manual discounts.",
            PermissionCategory.SALES,
            PermissionRiskLevel.HIGH,
            requiresAudit = true,
            requiresReason = true
        ),
        active(
            SALES_ITEMS_CHANGE_STATUS,
            "Change sale item status",
            "Change status of sale items.",
            PermissionCategory.SALES,
            PermissionRiskLevel.MEDIUM,
            requiresAudit = true
        ),

        // Payments / receivables.
        active(
            PAYMENTS_VIEW,
            "View payments",
            "View payment records.",
            PermissionCategory.PAYMENTS,
            PermissionRiskLevel.LOW
        ),
        active(
            PAYMENTS_COLLECT,
            "Collect payments",
            "Register payment collection.",
            PermissionCategory.PAYMENTS,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        active(
            PAYMENTS_PARTIAL_COLLECT,
            "Collect partial payments",
            "Register partial payment collection.",
            PermissionCategory.PAYMENTS,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        active(
            PAYMENTS_MARK_AS_CREDIT,
            "Mark sale as credit",
            "Leave balance as account receivable.",
            PermissionCategory.PAYMENTS,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        active(
            PAYMENTS_REFUND,
            "Refund payment",
            "Register payment refund.",
            PermissionCategory.PAYMENTS,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        active(
            PAYMENTS_REVERSE,
            "Reverse payment",
            "Reverse a payment due to operational error.",
            PermissionCategory.PAYMENTS,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        active(
            RECEIVABLES_VIEW,
            "View receivables",
            "View accounts receivable.",
            PermissionCategory.RECEIVABLES,
            PermissionRiskLevel.LOW
        ),
        active(
            RECEIVABLES_CREATE,
            "Create receivables",
            "Create accounts receivable.",
            PermissionCategory.RECEIVABLES,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        active(
            RECEIVABLES_REGISTER_PAYMENT,
            "Collect receivable",
            "Register payment against receivable.",
            PermissionCategory.RECEIVABLES,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),

        // Cash.
        active(CASH_VIEW, "View cash", "View cash information.", PermissionCategory.CASH, PermissionRiskLevel.LOW),
        active(
            CASH_OPEN,
            "Open cash legacy",
            "Open a cash session. Legacy alias.",
            PermissionCategory.CASH,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        active(
            CASH_CLOSE,
            "Close cash legacy",
            "Close a cash session. Legacy alias.",
            PermissionCategory.CASH,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        active(
            CASH_SESSION_VIEW_CURRENT,
            "View current cash session",
            "View current cash session.",
            PermissionCategory.CASH,
            PermissionRiskLevel.LOW
        ),
        active(
            CASH_SESSION_VIEW_HISTORY,
            "View cash session history",
            "View historical cash sessions.",
            PermissionCategory.CASH,
            PermissionRiskLevel.MEDIUM
        ),
        active(
            CASH_SESSION_OPEN,
            "Open cash session",
            "Open cash session.",
            PermissionCategory.CASH,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        active(
            CASH_SESSION_CLOSE,
            "Close cash session",
            "Close cash session.",
            PermissionCategory.CASH,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true
        ),
        active(
            CASH_MOVEMENTS_REGISTER_INFLOW,
            "Register cash inflow",
            "Register cash inflow.",
            PermissionCategory.CASH,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        active(
            CASH_MOVEMENTS_REGISTER_OUTFLOW,
            "Register cash outflow",
            "Register cash outflow.",
            PermissionCategory.CASH,
            PermissionRiskLevel.HIGH,
            requiresAudit = true,
            requiresReason = true
        ),
        active(
            CASH_MOVEMENTS_ADJUST,
            "Adjust cash",
            "Register cash adjustment.",
            PermissionCategory.CASH,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),

        // Inventory.
        active(
            INVENTORY_VIEW, "View inventory", "View inventory.", PermissionCategory.INVENTORY, PermissionRiskLevel.LOW
        ),
        active(
            INVENTORY_ADJUST,
            "Adjust inventory",
            "Adjust inventory manually.",
            PermissionCategory.INVENTORY,
            PermissionRiskLevel.HIGH,
            requiresAudit = true,
            requiresReason = true
        ),

        // Documents.
        active(
            DOCUMENTS_VIEW,
            "View documents",
            "View commercial documents.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.LOW
        ),
        active(
            DOCUMENTS_GENERATE_INTERNAL_TICKET,
            "Generate internal ticket",
            "Generate internal operation ticket.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.MEDIUM,
            requiresAudit = true
        ),
        active(
            DOCUMENTS_GENERATE_PHYSICAL_SALE_NOTE_REGISTRY,
            "Register physical sale note",
            "Register physical sale note metadata.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        active(
            DOCUMENTS_ISSUE_ELECTRONIC_INVOICE,
            "Issue electronic invoice",
            "Issue an electronic invoice.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        active(
            DOCUMENTS_DOWNLOAD_PDF,
            "Download document PDF",
            "Download document PDF or RIDE.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.LOW
        ),
        active(
            DOCUMENTS_DOWNLOAD_XML,
            "Download document XML",
            "Download document XML.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.MEDIUM
        ),
        active(
            DOCUMENTS_INVOICE_ISSUE,
            "Issue invoice",
            "Issue invoice using invoice module alias.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        active(
            DOCUMENTS_INVOICE_DOWNLOAD_XML,
            "Download invoice XML",
            "Download invoice XML.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.MEDIUM
        ),
        active(
            DOCUMENTS_INVOICE_DOWNLOAD_RIDE,
            "Download invoice RIDE",
            "Download invoice RIDE.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.LOW
        ),
        active(
            DOCUMENTS_INVOICE_REQUEST_CANCELLATION,
            "Request invoice cancellation",
            "Request electronic invoice cancellation.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        active(
            DOCUMENTS_ELECTRONIC_INVOICE_VIEW,
            "View electronic invoice",
            "View electronic invoice detail without downloading protected artifacts.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.LOW
        ),
        active(
            DOCUMENTS_ELECTRONIC_INVOICE_LIST,
            "List electronic invoices",
            "List electronic invoices for an organization.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.LOW
        ),
        active(
            DOCUMENTS_ELECTRONIC_INVOICE_ISSUE,
            "Issue electronic invoice",
            "Issue electronic invoices through the SRI backend flow.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        active(
            DOCUMENTS_ELECTRONIC_INVOICE_RETRY,
            "Retry electronic invoice authorization",
            "Retry controlled SRI authorization for an electronic invoice.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        active(
            DOCUMENTS_ELECTRONIC_INVOICE_DOWNLOAD_XML,
            "Download electronic invoice XML",
            "Download signed or authorized XML for an electronic invoice.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.MEDIUM,
            requiresAudit = true
        ),
        active(
            DOCUMENTS_ELECTRONIC_INVOICE_DOWNLOAD_RIDE,
            "Download electronic invoice RIDE",
            "Download electronic invoice RIDE PDF.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.LOW,
            requiresAudit = true
        ),
        active(
            DOCUMENTS_ELECTRONIC_INVOICE_EMAIL,
            "Email electronic invoice",
            "Send or resend electronic invoice artifacts by email.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.MEDIUM,
            requiresAudit = true
        ),
        active(
            DOCUMENTS_ELECTRONIC_INVOICE_VIEW_ERRORS,
            "View electronic invoice errors",
            "View SRI and delivery error details for electronic invoices.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.MEDIUM
        ),
        active(
            DOCUMENTS_ELECTRONIC_INVOICE_VIEW_AUDIT,
            "View electronic invoice audit",
            "View operational timeline and audit events for electronic invoices.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.MEDIUM
        ),
        active(
            DOCUMENTS_ELECTRONIC_INVOICE_HOMOLOGATE,
            "Run SRI homologation",
            "Run SRI homologation checks from Admin API.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.HIGH,
            requiresAudit = true,
            requiresReason = true
        ),
        active(
            DOCUMENTS_ELECTRONIC_INVOICE_ENABLE_PRODUCTION,
            "Enable electronic invoice production",
            "Enable production environment for electronic invoicing after readiness gates pass.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        active(
            DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS,
            "Manage SRI electronic invoice settings",
            "Manage SRI settings, signature activation and electronic sequences.",
            PermissionCategory.DOCUMENTS,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),

        // Tax.
        active(
            TAX_MANAGE,
            "Manage tax legacy",
            "Manage tax configuration. Legacy alias.",
            PermissionCategory.TAX,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        active(
            TAX_SETTINGS_VIEW,
            "View tax settings",
            "View organization tax settings.",
            PermissionCategory.TAX,
            PermissionRiskLevel.MEDIUM
        ),
        active(
            TAX_SETTINGS_UPDATE_ORGANIZATION_REGIME,
            "Update organization tax regime",
            "Update organization tax settings or regime.",
            PermissionCategory.TAX,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        active(
            TAX_PROFILES_ASSIGN_TO_ITEM,
            "Assign tax profile",
            "Assign tax profile to catalog item.",
            PermissionCategory.TAX,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),

        // Signature.
        active(
            SIGNATURE_VIEW_METADATA,
            "View signature metadata",
            "View electronic signature metadata.",
            PermissionCategory.SIGNATURE,
            PermissionRiskLevel.MEDIUM
        ),
        active(
            SIGNATURE_UPLOAD,
            "Upload signature",
            "Upload electronic signature file.",
            PermissionCategory.SIGNATURE,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        active(
            SIGNATURE_REPLACE,
            "Replace signature",
            "Replace active electronic signature.",
            PermissionCategory.SIGNATURE,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        active(
            SIGNATURE_REVOKE,
            "Revoke signature",
            "Revoke electronic signature.",
            PermissionCategory.SIGNATURE,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        active(
            SIGNATURE_TEST,
            "Test signature",
            "Test electronic signature.",
            PermissionCategory.SIGNATURE,
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        active(
            SIGNATURE_USE_FOR_INVOICING,
            "Use signature for invoicing",
            "Use electronic signature to issue invoices.",
            PermissionCategory.SIGNATURE,
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        active(
            SIGNATURE_VIEW_AUDIT,
            "View signature audit",
            "View electronic signature audit trail.",
            PermissionCategory.SIGNATURE,
            PermissionRiskLevel.HIGH
        ),

        // Reports / audit.
        active(
            REPORTS_DASHBOARD_VIEW,
            "View dashboard",
            "View dashboard report.",
            PermissionCategory.REPORTS,
            PermissionRiskLevel.LOW
        ),
        active(
            REPORTS_SALES_VIEW,
            "View sales reports",
            "View sales reports.",
            PermissionCategory.REPORTS,
            PermissionRiskLevel.LOW
        ),
        active(
            REPORTS_CASH_VIEW,
            "View cash reports",
            "View cash reports.",
            PermissionCategory.REPORTS,
            PermissionRiskLevel.MEDIUM
        ),
        active(
            REPORTS_TAX_VIEW,
            "View tax reports",
            "View tax reports.",
            PermissionCategory.REPORTS,
            PermissionRiskLevel.MEDIUM
        ),
        active(
            REPORTS_DOCUMENTS_VIEW,
            "View document reports",
            "View document reports.",
            PermissionCategory.REPORTS,
            PermissionRiskLevel.MEDIUM
        ),
        active(AUDIT_VIEW, "View audit", "View audit log.", PermissionCategory.AUDIT, PermissionRiskLevel.HIGH),

        // Future discovery foundation: active in backend, hidden by feature flag until product turns it on.
        flagged(
            CATALOG_PRICE_VIEW,
            "View catalog price",
            "View catalog price metadata.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled"
        ),
        flagged(
            CATALOG_PRICE_UPDATE,
            "Update catalog price",
            "Update public-ready catalog price metadata.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled",
            PermissionRiskLevel.HIGH,
            requiresAudit = true,
            requiresReason = true
        ),
        flagged(
            CATALOG_PRICE_HISTORY_VIEW,
            "View price history",
            "View catalog price history.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled",
            PermissionRiskLevel.MEDIUM
        ),
        flagged(
            CATALOG_SEARCH_METADATA_VIEW,
            "View search metadata",
            "View catalog search metadata.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled"
        ),
        flagged(
            CATALOG_SEARCH_METADATA_UPDATE,
            "Update search metadata",
            "Update catalog search metadata.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled",
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        flagged(
            CATALOG_PUBLIC_VISIBILITY_VIEW,
            "View public visibility",
            "View catalog public visibility settings.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled"
        ),
        flagged(
            CATALOG_PUBLIC_VISIBILITY_UPDATE,
            "Update public visibility",
            "Update catalog public visibility.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled",
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        flagged(
            CATALOG_PUBLIC_VISIBILITY_HIDE,
            "Hide public item",
            "Hide a public catalog item.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled",
            PermissionRiskLevel.HIGH,
            requiresAudit = true,
            requiresReason = true
        ),
        flagged(
            CATALOG_PUBLIC_VISIBILITY_REPUBLISH,
            "Republish public item",
            "Republish a hidden public catalog item.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled",
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        flagged(
            CATALOG_PUBLIC_DISPLAY_UPDATE_PRICE_MODE,
            "Update public price mode",
            "Update public price display mode.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled",
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        flagged(
            CATALOG_PUBLIC_DISPLAY_UPDATE_STOCK_MODE,
            "Update public stock mode",
            "Update public stock display mode.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled",
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        flagged(
            CATALOG_PUBLIC_DISPLAY_ALLOW_COMPARISON,
            "Allow public comparison",
            "Allow public comparison for an item.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled",
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        flagged(
            CATALOG_PUBLIC_PREVIEW_VIEW,
            "View public preview",
            "View future public discovery preview.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled"
        ),
        flagged(
            BRANCH_LOCATION_VIEW,
            "View branch location",
            "View branch location.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled"
        ),
        flagged(
            BRANCH_LOCATION_UPDATE,
            "Update branch location",
            "Update branch location.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled",
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        flagged(
            BRANCH_LOCATION_PUBLIC_VISIBILITY_VIEW,
            "View public branch location",
            "View public location settings.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled"
        ),
        flagged(
            BRANCH_LOCATION_PUBLIC_VISIBILITY_UPDATE,
            "Update public branch location",
            "Update public location visibility.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled",
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        flagged(
            BRANCH_LOCATION_PRIVACY_MODE_UPDATE,
            "Update location privacy mode",
            "Update branch location privacy mode.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled",
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        flagged(
            DISCOVERY_SETTINGS_VIEW,
            "View discovery settings",
            "View discovery settings.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled"
        ),
        flagged(
            DISCOVERY_SETTINGS_UPDATE,
            "Update discovery settings",
            "Update discovery settings.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled",
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true,
            requiresReason = true,
            requiresStepUp = true
        ),
        flagged(
            DISCOVERY_PREVIEW_VIEW,
            "View discovery preview",
            "View discovery preview.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled"
        ),
        flagged(
            DISCOVERY_AUDIT_VIEW,
            "View discovery audit",
            "View discovery audit.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled",
            PermissionRiskLevel.HIGH
        ),
        flagged(
            BUSINESS_HOURS_VIEW,
            "View business hours",
            "View business hours.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled"
        ),
        flagged(
            BUSINESS_HOURS_UPDATE,
            "Update business hours",
            "Update business hours.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled",
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),
        flagged(
            BUSINESS_HOURS_PUBLIC_VISIBILITY_UPDATE,
            "Update public business hours",
            "Update public business hours visibility.",
            PermissionCategory.DISCOVERY,
            "public_discovery_enabled",
            PermissionRiskLevel.HIGH,
            requiresAudit = true
        ),

        // Future assisted commerce: reserved. Seeded for forward compatibility but denied by default.
        reserved(
            CONSUMER_REQUEST_LIST_VIEW,
            "View consumer request lists",
            "Reserved future permission.",
            PermissionCategory.ASSISTED_COMMERCE,
            PermissionScope.CONSUMER,
            "assisted_commerce_enabled"
        ),
        reserved(
            CONSUMER_REQUEST_LIST_CREATE,
            "Create consumer request lists",
            "Reserved future permission.",
            PermissionCategory.ASSISTED_COMMERCE,
            PermissionScope.CONSUMER,
            "assisted_commerce_enabled"
        ),
        reserved(
            CONSUMER_SEARCH_CREATE,
            "Create consumer search",
            "Reserved future permission.",
            PermissionCategory.ASSISTED_COMMERCE,
            PermissionScope.CONSUMER,
            "assisted_commerce_enabled"
        ),
        reserved(
            CONSUMER_STORE_REQUEST_CREATE,
            "Create store request",
            "Reserved future permission.",
            PermissionCategory.ASSISTED_COMMERCE,
            PermissionScope.CONSUMER,
            "assisted_commerce_enabled"
        ),
        reserved(
            STORE_REQUESTS_VIEW,
            "View store requests",
            "Reserved future permission.",
            PermissionCategory.ASSISTED_COMMERCE,
            PermissionScope.ORGANIZATION,
            "assisted_commerce_enabled"
        ),
        reserved(
            STORE_REQUESTS_ACCEPT,
            "Accept store requests",
            "Reserved future permission.",
            PermissionCategory.ASSISTED_COMMERCE,
            PermissionScope.ORGANIZATION,
            "assisted_commerce_enabled",
            PermissionRiskLevel.HIGH
        ),
        reserved(
            STORE_REQUESTS_REJECT,
            "Reject store requests",
            "Reserved future permission.",
            PermissionCategory.ASSISTED_COMMERCE,
            PermissionScope.ORGANIZATION,
            "assisted_commerce_enabled",
            PermissionRiskLevel.HIGH
        ),
        reserved(
            STORE_REQUESTS_CLOSE,
            "Close store requests",
            "Reserved future permission.",
            PermissionCategory.ASSISTED_COMMERCE,
            PermissionScope.ORGANIZATION,
            "assisted_commerce_enabled",
            PermissionRiskLevel.HIGH
        ),
        reserved(
            STORE_REQUESTS_QUOTES_CREATE,
            "Create store request quotes",
            "Reserved future permission.",
            PermissionCategory.ASSISTED_COMMERCE,
            PermissionScope.ORGANIZATION,
            "assisted_commerce_enabled",
            PermissionRiskLevel.HIGH
        ),
        reserved(
            STORE_REQUESTS_CONVERT_TO_SALE,
            "Convert store request to sale",
            "Reserved future permission.",
            PermissionCategory.ASSISTED_COMMERCE,
            PermissionScope.ORGANIZATION,
            "assisted_commerce_enabled",
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true
        ),
        reserved(
            STORE_REQUESTS_CONVERT_TO_RESERVATION,
            "Convert store request to reservation",
            "Reserved future permission.",
            PermissionCategory.ASSISTED_COMMERCE,
            PermissionScope.ORGANIZATION,
            "assisted_commerce_enabled",
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true
        ),
        reserved(
            CHAT_CONVERSATIONS_VIEW,
            "View conversations",
            "Reserved future permission.",
            PermissionCategory.ASSISTED_COMMERCE,
            PermissionScope.ORGANIZATION,
            "assisted_commerce_enabled"
        ),
        reserved(
            CHAT_MESSAGES_SEND,
            "Send chat messages",
            "Reserved future permission.",
            PermissionCategory.ASSISTED_COMMERCE,
            PermissionScope.ORGANIZATION,
            "assisted_commerce_enabled",
            PermissionRiskLevel.MEDIUM
        ),
        reserved(
            CHAT_ATTACHMENTS_UPLOAD,
            "Upload chat attachments",
            "Reserved future permission.",
            PermissionCategory.ASSISTED_COMMERCE,
            PermissionScope.ORGANIZATION,
            "assisted_commerce_enabled",
            PermissionRiskLevel.HIGH
        ),
        reserved(
            PLATFORM_ASSISTED_COMMERCE_MODERATE,
            "Moderate assisted commerce",
            "Reserved future platform moderation permission.",
            PermissionCategory.ASSISTED_COMMERCE,
            PermissionScope.PLATFORM,
            "assisted_commerce_enabled",
            PermissionRiskLevel.CRITICAL,
            requiresAudit = true
        ),
    )

    val known: Set<String> = definitions.map { it.code }.toSet() + ALL

    val active: List<PermissionDefinition> = definitions.filter { it.status == PermissionStatus.ACTIVE }

    val reserved: List<PermissionDefinition> = definitions.filter { it.status == PermissionStatus.RESERVED }

    fun requireKnown(code: String): PermissionDefinition {
        return definitions.firstOrNull { it.code == code } ?: error("Unknown permission code: $code")
    }

    private fun active(
        code: String,
        name: String,
        description: String,
        category: PermissionCategory,
        riskLevel: PermissionRiskLevel = PermissionRiskLevel.LOW,
        scope: PermissionScope = PermissionScope.ORGANIZATION,
        requiresAudit: Boolean = false,
        requiresReason: Boolean = false,
        requiresStepUp: Boolean = false,
    ): PermissionDefinition = PermissionDefinition(
        code = code,
        name = name,
        description = description,
        category = category,
        scope = scope,
        riskLevel = riskLevel,
        status = PermissionStatus.ACTIVE,
        systemManaged = true,
        requiresAudit = requiresAudit,
        requiresReason = requiresReason,
        requiresStepUp = requiresStepUp,
    )

    private fun critical(
        code: String,
        name: String,
        description: String,
        category: PermissionCategory,
        requiresAudit: Boolean = true,
        requiresReason: Boolean = false,
        requiresStepUp: Boolean = false,
        scope: PermissionScope = PermissionScope.ORGANIZATION,
    ): PermissionDefinition = active(
        code = code,
        name = name,
        description = description,
        category = category,
        riskLevel = PermissionRiskLevel.CRITICAL,
        scope = scope,
        requiresAudit = requiresAudit,
        requiresReason = requiresReason,
        requiresStepUp = requiresStepUp,
    )

    private fun platform(
        code: String,
        name: String,
        description: String,
        category: PermissionCategory,
        riskLevel: PermissionRiskLevel,
    ): PermissionDefinition = PermissionDefinition(
        code = code,
        name = name,
        description = description,
        category = category,
        scope = PermissionScope.PLATFORM,
        riskLevel = riskLevel,
        status = PermissionStatus.ACTIVE,
        systemManaged = true,
        requiresAudit = true,
        requiresReason = true,
        requiresStepUp = riskLevel == PermissionRiskLevel.CRITICAL,
    )

    private fun flagged(
        code: String,
        name: String,
        description: String,
        category: PermissionCategory,
        featureFlag: String,
        riskLevel: PermissionRiskLevel = PermissionRiskLevel.LOW,
        requiresAudit: Boolean = false,
        requiresReason: Boolean = false,
        requiresStepUp: Boolean = false,
    ): PermissionDefinition = PermissionDefinition(
        code = code,
        name = name,
        description = description,
        category = category,
        scope = PermissionScope.ORGANIZATION,
        riskLevel = riskLevel,
        status = PermissionStatus.ACTIVE,
        systemManaged = true,
        requiresAudit = requiresAudit,
        requiresReason = requiresReason,
        requiresStepUp = requiresStepUp,
        featureFlag = featureFlag,
    )

    private fun reserved(
        code: String,
        name: String,
        description: String,
        category: PermissionCategory,
        scope: PermissionScope,
        featureFlag: String,
        riskLevel: PermissionRiskLevel = PermissionRiskLevel.LOW,
        requiresAudit: Boolean = false,
    ): PermissionDefinition = PermissionDefinition(
        code = code,
        name = name,
        description = description,
        category = category,
        scope = scope,
        riskLevel = riskLevel,
        status = PermissionStatus.RESERVED,
        systemManaged = true,
        requiresAudit = requiresAudit || riskLevel == PermissionRiskLevel.CRITICAL,
        requiresReason = riskLevel == PermissionRiskLevel.CRITICAL,
        requiresStepUp = riskLevel == PermissionRiskLevel.CRITICAL,
        featureFlag = featureFlag,
    )
}
