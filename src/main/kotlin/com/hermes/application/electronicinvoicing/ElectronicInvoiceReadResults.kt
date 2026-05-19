package com.hermes.application.electronicinvoicing

data class ElectronicInvoiceResult(
    val record: ElectronicInvoiceIssueRecord,
)

data class ElectronicInvoicesResult(
    val records: List<ElectronicInvoiceIssueRecord>,
)
