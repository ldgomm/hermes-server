package com.hermes.application.documents

import com.hermes.domain.document.CommercialDocument
import com.hermes.domain.money.Money
import java.nio.charset.StandardCharsets

class SimpleCommercialDocumentPdfRenderer : CommercialDocumentPdfRenderer {
    override fun render(document: CommercialDocument): CommercialDocumentFile {
        val title = when (document.documentType.storageValue) {
            "internal_ticket" -> "Ticket interno"
            "physical_sale_note_registry" -> "Registro de nota de venta fisica"
            else -> "Documento comercial"
        }

        val lines = buildList {
            add(title)
            add("Documento: ${document.documentNumber}")
            add("Fecha: ${document.issuedAt}")
            add("Organizacion: ${document.organizationId}")
            add("Sucursal: ${document.branchId}")
            add("Venta: ${document.saleId ?: "N/A"}")
            add("")
            add("Detalle")
            document.lineSnapshots.forEachIndexed { index, item ->
                add("${index + 1}. ${item.description}")
                add("   Cantidad: ${item.quantity.value.toPlainString()} ${item.quantity.unitCode}")
                add("   P.Unit: ${item.unitPrice.print()}  Desc: ${item.discount.print()}  Total: ${item.lineTotal.print()}")
            }
            add("")
            add("Subtotal: ${document.totalsSnapshot.subtotal.print()}")
            add("Descuento: ${document.totalsSnapshot.discount.print()}")
            add("Impuestos: ${document.totalsSnapshot.taxTotal.print()}")
            add("TOTAL: ${document.totalsSnapshot.grandTotal.print()}")
            add("Pagado: ${document.totalsSnapshot.paidAmount.print()} (${document.totalsSnapshot.paymentStatus})")
            document.notes?.let { add("Notas: $it") }
            add("")
            add("Documento operativo interno. No reemplaza una factura electronica autorizada por el SRI.")
        }

        val bytes = MinimalPdf.write(lines)
        val safeNumber = document.documentNumber.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val objectKey = "commercial-documents/${document.organizationId}/${document.id}/$safeNumber.pdf"
        return CommercialDocumentFile(
            objectKey = objectKey,
            filename = "$safeNumber.pdf",
            contentType = "application/pdf",
            bytes = bytes,
        )
    }

    private fun Money.print(): String = "${amount.toPlainString()} ${currency.value}"
}

private object MinimalPdf {
    fun write(lines: List<String>): ByteArray {
        val content = buildString {
            append("BT\n/F1 11 Tf\n50 790 Td\n")
            lines.forEachIndexed { index, line ->
                if (index > 0) append("0 -15 Td\n")
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
            .take(130)
}
