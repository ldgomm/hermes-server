package com.hermes.infrastructure.xml

import com.hermes.application.electronicinvoicing.*
import java.time.format.DateTimeFormatter

class SriInvoiceXmlBuilder : InvoiceXmlBuilder {
    override fun build(command: BuildSriInvoiceXmlCommand): GeneratedXml {
        val xml = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<factura id=\"comprobante\" version=\"${command.schemaVersion.version}\">")
            appendInfoTributaria(command.infoTributaria)
            appendInfoFactura(command.infoFactura)
            appendDetalles(command.detalles)
            appendInfoAdicional(command.infoAdicional)
            append("</factura>")
        }
        return GeneratedXml.of(command.schemaVersion, xml)
    }

    private fun StringBuilder.appendInfoTributaria(info: SriInvoiceTaxInfo) {
        appendLine("  <infoTributaria>")
        appendTag("ambiente", info.environment.code, 4)
        appendTag("tipoEmision", info.emissionType.code, 4)
        appendTag("razonSocial", info.razonSocial, 4)
        info.nombreComercial?.trim()?.takeIf { it.isNotBlank() }?.let { appendTag("nombreComercial", it, 4) }
        appendTag("ruc", info.ruc, 4)
        appendTag("claveAcceso", info.accessKey.value, 4)
        appendTag("codDoc", info.documentType.code, 4)
        appendTag("estab", info.series.establishmentCode, 4)
        appendTag("ptoEmi", info.series.emissionPointCode, 4)
        appendTag("secuencial", info.sequential.formatted, 4)
        appendTag("dirMatriz", info.dirMatriz, 4)
        info.agenteRetencion?.let { appendTag("agenteRetencion", it, 4) }
        info.contribuyenteRimpe.xmlValue?.let { appendTag("contribuyenteRimpe", it, 4) }
        appendLine("  </infoTributaria>")
    }

    private fun StringBuilder.appendInfoFactura(info: SriInvoiceInfo) {
        appendLine("  <infoFactura>")
        appendTag("fechaEmision", info.fechaEmision.format(invoiceDateFormatter), 4)
        info.dirEstablecimiento?.trim()?.takeIf { it.isNotBlank() }?.let { appendTag("dirEstablecimiento", it, 4) }
        info.contribuyenteEspecial?.trim()?.takeIf { it.isNotBlank() }
            ?.let { appendTag("contribuyenteEspecial", it, 4) }
        info.obligadoContabilidad?.let { appendTag("obligadoContabilidad", it.xmlValue, 4) }
        appendTag("tipoIdentificacionComprador", info.buyerIdentificationType.code, 4)
        appendTag("razonSocialComprador", info.buyerLegalName, 4)
        appendTag("identificacionComprador", info.buyerIdentification, 4)
        info.buyerAddress?.trim()?.takeIf { it.isNotBlank() }?.let { appendTag("direccionComprador", it, 4) }
        appendTag("totalSinImpuestos", SriDecimalFormatter.money(info.totalSinImpuestos), 4)
        appendTag("totalDescuento", SriDecimalFormatter.money(info.totalDescuento), 4)
        appendTotalConImpuestos(info.totalConImpuestos)
        appendTag("propina", SriDecimalFormatter.money(info.propina), 4)
        appendTag("importeTotal", SriDecimalFormatter.money(info.importeTotal), 4)
        appendTag("moneda", info.moneda, 4)
        appendPagos(info.pagos)
        appendLine("  </infoFactura>")
    }

    private fun StringBuilder.appendTotalConImpuestos(taxes: List<SriInvoiceTotalTax>) {
        appendLine("    <totalConImpuestos>")
        taxes.forEach { tax ->
            appendLine("      <totalImpuesto>")
            appendTag("codigo", tax.codigo, 8)
            appendTag("codigoPorcentaje", tax.codigoPorcentaje, 8)
            tax.descuentoAdicional?.let { appendTag("descuentoAdicional", SriDecimalFormatter.money(it), 8) }
            appendTag("baseImponible", SriDecimalFormatter.money(tax.baseImponible), 8)
            appendTag("tarifa", SriDecimalFormatter.rate(tax.tarifa), 8)
            appendTag("valor", SriDecimalFormatter.money(tax.valor), 8)
            appendLine("      </totalImpuesto>")
        }
        appendLine("    </totalConImpuestos>")
    }

    private fun StringBuilder.appendPagos(payments: List<SriInvoicePayment>) {
        appendLine("    <pagos>")
        payments.forEach { payment ->
            appendLine("      <pago>")
            appendTag("formaPago", payment.formaPago.code, 8)
            appendTag("total", SriDecimalFormatter.money(payment.total), 8)
            payment.plazo?.let { appendTag("plazo", SriDecimalFormatter.term(it), 8) }
            payment.unidadTiempo?.trim()?.takeIf { it.isNotBlank() }?.let { appendTag("unidadTiempo", it, 8) }
            appendLine("      </pago>")
        }
        appendLine("    </pagos>")
    }

    private fun StringBuilder.appendDetalles(details: List<SriInvoiceDetail>) {
        appendLine("  <detalles>")
        details.forEach { detail ->
            appendLine("    <detalle>")
            appendTag("codigoPrincipal", detail.codigoPrincipal, 6)
            detail.codigoAuxiliar?.trim()?.takeIf { it.isNotBlank() }?.let { appendTag("codigoAuxiliar", it, 6) }
            appendTag("descripcion", detail.descripcion, 6)
            detail.unidadMedida?.trim()?.takeIf { it.isNotBlank() }?.let { appendTag("unidadMedida", it, 6) }
            appendTag("cantidad", SriDecimalFormatter.quantity(detail.cantidad), 6)
            appendTag("precioUnitario", SriDecimalFormatter.unitPrice(detail.precioUnitario), 6)
            appendTag("descuento", SriDecimalFormatter.money(detail.descuento), 6)
            appendTag("precioTotalSinImpuesto", SriDecimalFormatter.money(detail.precioTotalSinImpuesto), 6)
            appendDetallesAdicionales(detail.detallesAdicionales)
            appendDetailTaxes(detail.impuestos)
            appendLine("    </detalle>")
        }
        appendLine("  </detalles>")
    }

    private fun StringBuilder.appendDetallesAdicionales(additional: List<SriInvoiceDetailAdditional>) {
        if (additional.isEmpty()) return
        appendLine("      <detallesAdicionales>")
        additional.forEach { item ->
            val name = XmlEscaper.attribute(item.nombre)
            val value = XmlEscaper.attribute(item.valor)
            appendLine("        <detAdicional nombre=\"$name\" valor=\"$value\"/>")
        }
        appendLine("      </detallesAdicionales>")
    }

    private fun StringBuilder.appendDetailTaxes(taxes: List<SriInvoiceDetailTax>) {
        appendLine("      <impuestos>")
        taxes.forEach { tax ->
            appendLine("        <impuesto>")
            appendTag("codigo", tax.codigo, 10)
            appendTag("codigoPorcentaje", tax.codigoPorcentaje, 10)
            appendTag("tarifa", SriDecimalFormatter.rate(tax.tarifa), 10)
            appendTag("baseImponible", SriDecimalFormatter.money(tax.baseImponible), 10)
            appendTag("valor", SriDecimalFormatter.money(tax.valor), 10)
            appendLine("        </impuesto>")
        }
        appendLine("      </impuestos>")
    }

    private fun StringBuilder.appendInfoAdicional(additionalFields: List<SriInvoiceAdditionalField>) {
        if (additionalFields.isEmpty()) return
        appendLine("  <infoAdicional>")
        additionalFields.forEach { field ->
            val name = XmlEscaper.attribute(field.nombre)
            val value = XmlEscaper.text(field.valor)
            appendLine("    <campoAdicional nombre=\"$name\">$value</campoAdicional>")
        }
        appendLine("  </infoAdicional>")
    }

    private fun StringBuilder.appendTag(name: String, rawValue: String, spaces: Int) {
        val indent = " ".repeat(spaces)
        appendLine("$indent<$name>${XmlEscaper.text(rawValue)}</$name>")
    }

    companion object {
        private val invoiceDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
}
