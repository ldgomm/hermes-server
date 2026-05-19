package com.hermes.application.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

class SimpleSriRidePdfRenderer : ElectronicInvoiceRideRenderer {
    override fun render(command: ElectronicInvoiceRideRenderCommand): ElectronicInvoiceGeneratedFile {
        val data = SriRideXmlParser.parse(command.authorizedXml, command.record)
        val lines = buildList {
            add("RIDE - Representacion impresa de factura electronica")
            add("Documento autorizado por el SRI")
            add("Autorizacion: ${data.authorizationNumber}")
            data.authorizationDate?.let { add("Fecha autorizacion: $it") }
            add("Clave de acceso: ${data.accessKey}")
            add("Numero: ${data.series}-${data.sequential}")
            add("Fecha emision: ${data.issuedDate}")
            add("")
            add("Emisor")
            add(data.issuerName)
            data.commercialName?.let { add("Nombre comercial: $it") }
            add("RUC: ${data.ruc}")
            add("Matriz: ${data.mainAddress}")
            data.rimpeLegend?.let { add(it) }
            add("")
            add("Comprador")
            add(data.buyerName)
            add("Identificacion: ${data.buyerIdentification}")
            data.buyerAddress?.let { add("Direccion: $it") }
            add("")
            add("Detalle")
            data.details.take(MAX_DETAIL_LINES).forEachIndexed { index, detail ->
                add("${index + 1}. ${detail.description}")
                add("   Cant: ${detail.quantity}  P.Unit: ${detail.unitPrice}  Desc: ${detail.discount}  Total: ${detail.totalWithoutTax}")
            }
            if (data.details.size > MAX_DETAIL_LINES) add("... ${data.details.size - MAX_DETAIL_LINES} lineas adicionales")
            add("")
            add("Subtotal sin impuestos: ${data.totalWithoutTaxes}")
            add("Descuento: ${data.totalDiscount}")
            data.taxes.forEach { tax -> add("Impuesto ${tax.code}/${tax.rateCode}: Base ${tax.base} Valor ${tax.value}") }
            data.tip?.let { add("Propina: $it") }
            add("TOTAL: ${data.total}")
            add("")
            add("Este RIDE no reemplaza el XML autorizado. Conserve el XML para validacion tributaria.")
        }

        val safeNumber = command.record.documentNumber.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return ElectronicInvoiceGeneratedFile(
            filename = "${safeNumber}_RIDE.pdf",
            contentType = "application/pdf",
            bytes = MinimalRidePdf.write(lines),
        )
    }

    private companion object {
        const val MAX_DETAIL_LINES = 18
    }
}

private data class SriRideData(
    val authorizationNumber: String,
    val authorizationDate: String?,
    val accessKey: String,
    val series: String,
    val sequential: String,
    val issuedDate: String,
    val issuerName: String,
    val commercialName: String?,
    val ruc: String,
    val mainAddress: String,
    val rimpeLegend: String?,
    val buyerName: String,
    val buyerIdentification: String,
    val buyerAddress: String?,
    val details: List<SriRideDetail>,
    val taxes: List<SriRideTax>,
    val totalWithoutTaxes: String,
    val totalDiscount: String,
    val tip: String?,
    val total: String,
)

private data class SriRideDetail(
    val description: String,
    val quantity: String,
    val unitPrice: String,
    val discount: String,
    val totalWithoutTax: String,
)

private data class SriRideTax(
    val code: String,
    val rateCode: String,
    val base: String,
    val value: String,
)

private object SriRideXmlParser {
    fun parse(xml: ByteArray, record: ElectronicInvoiceIssueRecord): SriRideData {
        val outer = parseDocument(xml)
        val root = outer.documentElement
        val authorizationNumber = root.text("numeroAutorizacion") ?: record.authorizationNumber
        val authorizationDate = root.text("fechaAutorizacion")
        val invoiceXml = if (root.tagName.equals("autorizacion", ignoreCase = true)) {
            root.text("comprobante")
                ?: throw DomainRuleViolation("Authorized XML does not contain comprobante payload.")
        } else {
            String(xml, StandardCharsets.UTF_8)
        }
        val invoice = parseDocument(invoiceXml.toByteArray(StandardCharsets.UTF_8))
        val invoiceRoot = invoice.documentElement
        val infoTributaria = invoiceRoot.first("infoTributaria")
            ?: throw DomainRuleViolation("Invoice XML does not contain infoTributaria.")
        val infoFactura = invoiceRoot.first("infoFactura")
            ?: throw DomainRuleViolation("Invoice XML does not contain infoFactura.")

        return SriRideData(
            authorizationNumber = authorizationNumber,
            authorizationDate = authorizationDate,
            accessKey = infoTributaria.requiredText("claveAcceso"),
            series = infoTributaria.requiredText("estab") + "-" + infoTributaria.requiredText("ptoEmi"),
            sequential = infoTributaria.requiredText("secuencial"),
            issuedDate = infoFactura.requiredText("fechaEmision"),
            issuerName = infoTributaria.requiredText("razonSocial"),
            commercialName = infoTributaria.text("nombreComercial"),
            ruc = infoTributaria.requiredText("ruc"),
            mainAddress = infoTributaria.requiredText("dirMatriz"),
            rimpeLegend = infoTributaria.text("contribuyenteRimpe"),
            buyerName = infoFactura.requiredText("razonSocialComprador"),
            buyerIdentification = infoFactura.requiredText("identificacionComprador"),
            buyerAddress = infoFactura.text("direccionComprador"),
            details = invoiceRoot.elements("detalle").map { detail ->
                SriRideDetail(
                    description = detail.requiredText("descripcion"),
                    quantity = detail.text("cantidad") ?: "0.00",
                    unitPrice = detail.text("precioUnitario") ?: "0.00",
                    discount = detail.text("descuento") ?: "0.00",
                    totalWithoutTax = detail.text("precioTotalSinImpuesto") ?: "0.00",
                )
            },
            taxes = infoFactura.elements("totalImpuesto").map { tax ->
                SriRideTax(
                    code = tax.text("codigo") ?: "",
                    rateCode = tax.text("codigoPorcentaje") ?: "",
                    base = tax.text("baseImponible") ?: "0.00",
                    value = tax.text("valor") ?: "0.00",
                )
            },
            totalWithoutTaxes = infoFactura.text("totalSinImpuestos") ?: "0.00",
            totalDiscount = infoFactura.text("totalDescuento") ?: "0.00",
            tip = infoFactura.text("propina"),
            total = infoFactura.requiredText("importeTotal"),
        )
    }

    private fun parseDocument(bytes: ByteArray): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { factory.setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        factory.isXIncludeAware = false
        factory.isExpandEntityReferences = false
        return factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes)).also { it.documentElement.normalize() }
    }

    private fun Element.first(tagName: String): Element? =
        getElementsByTagName(tagName).let { nodes -> nodes.item(0) as? Element }

    private fun Element.elements(tagName: String): List<Element> =
        getElementsByTagName(tagName).let { nodes ->
            (0 until nodes.length)
                .mapNotNull { index -> nodes.item(index) as? Element }
                .filter { it.tagName == tagName }
        }

    private fun Element.text(tagName: String): String? =
        first(tagName)?.textContent?.trim()?.takeIf { it.isNotBlank() }

    private fun Element.requiredText(tagName: String): String =
        text(tagName) ?: throw DomainRuleViolation("Invoice XML required field '$tagName' is missing.")
}

private object MinimalRidePdf {
    fun write(lines: List<String>): ByteArray {
        val content = buildString {
            append("BT\n/F1 10 Tf\n45 790 Td\n")
            lines.take(58).forEachIndexed { index, line ->
                if (index > 0) append("0 -13 Td\n")
                append("(").append(line.escapePdfText()).append(") Tj\n")
            }
            append("ET\n")
        }

        val objects = listOf(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
            "<< /Length ${content.toByteArray(StandardCharsets.UTF_8).size} >>\nstream\n$content\nendstream",
        )

        val out = StringBuilder("%PDF-1.4\n")
        val offsets = mutableListOf(0)
        objects.forEachIndexed { index, obj ->
            offsets += out.toString().toByteArray(StandardCharsets.UTF_8).size
            out.append(index + 1).append(" 0 obj\n")
            out.append(obj).append("\nendobj\n")
        }
        val xrefStart = out.toString().toByteArray(StandardCharsets.UTF_8).size
        out.append("xref\n0 ${objects.size + 1}\n")
        out.append("0000000000 65535 f \n")
        offsets.drop(1).forEach { offset -> out.append(offset.toString().padStart(10, '0')).append(" 00000 n \n") }
        out.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\n")
        out.append("startxref\n$xrefStart\n%%EOF")
        return out.toString().toByteArray(StandardCharsets.UTF_8)
    }

    private fun String.escapePdfText(): String =
        replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace(Regex("[\r\n\t]+"), " ")
            .take(145)
}
