package com.hermes.application.electronicinvoicing

interface InvoiceXmlBuilder {
    fun build(command: BuildSriInvoiceXmlCommand): GeneratedXml
}
