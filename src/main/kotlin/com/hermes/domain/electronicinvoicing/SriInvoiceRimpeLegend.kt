package com.hermes.domain.electronicinvoicing

enum class SriInvoiceRimpeLegend(val xmlValue: String?) {
    NONE(null),
    NEGOCIO_POPULAR_REGIMEN_RIMPE("CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE"),
    CONTRIBUYENTE_REGIMEN_RIMPE("CONTRIBUYENTE RÉGIMEN RIMPE");

    val shouldRender: Boolean get() = xmlValue != null
}
