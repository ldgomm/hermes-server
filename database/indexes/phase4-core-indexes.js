// Phase 4 closure indexes.
// Safe to run after M001-M028. Re-running should be harmless if names match.

db.sales.createIndex(
    {organizationId: 1, branchId: 1, operationalStatus: 1, createdAt: -1},
    {name: "sales_org_branch_status_created_idx"}
);

db.sales.createIndex(
    {organizationId: 1, saleNumber: 1},
    {name: "sales_org_sale_number_unique_idx", unique: true, sparse: true}
);

db.sales.createIndex(
    {organizationId: 1, "items.catalogItemId": 1, createdAt: -1},
    {name: "sales_org_item_created_idx"}
);

db.sales.createIndex(
    {organizationId: 1, paymentStatus: 1, collectionStatus: 1, dueAt: 1},
    {name: "sales_org_payment_collection_due_idx"}
);

db.payments.createIndex(
    {organizationId: 1, saleId: 1, status: 1, paidAt: -1},
    {name: "payments_org_sale_status_paid_idx"}
);

db.receivables.createIndex(
    {organizationId: 1, customerId: 1, status: 1, dueAt: 1},
    {name: "receivables_org_customer_status_due_idx"}
);

db.organization_catalog_items.createIndex(
    {organizationId: 1, "identifiers.normalizedValue": 1, "identifiers.type": 1},
    {name: "catalog_items_org_identifier_idx"}
);

db.stock_balances.createIndex(
    {organizationId: 1, catalogItemId: 1, branchId: 1},
    {name: "stock_balance_org_item_branch_unique_idx", unique: true}
);

db.commercial_documents.createIndex(
    {accessKey: 1},
    {name: "commercial_documents_access_key_unique_idx", unique: true, sparse: true}
);
