package com.hermes.application.electronicinvoicing

object ElectronicInvoiceHomologationReportFormatter {
    fun toMarkdown(report: ElectronicInvoiceHomologationReport): String = buildString {
        appendLine("# Reporte de homologación SRI — Factura electrónica")
        appendLine()
        appendLine("- Organización: `${report.organizationId}`")
        appendLine("- Ambiente: `${report.environment}`")
        appendLine("- Inicio: `${report.startedAt}`")
        appendLine("- Fin: `${report.finishedAt}`")
        appendLine("- Resultado: **${if (report.passed) "APROBADO" else "NO APROBADO"}**")
        appendLine()
        if (report.missingRequiredScenarioCodes.isNotEmpty()) {
            appendLine("## Escenarios obligatorios faltantes")
            report.missingRequiredScenarioCodes.sortedBy { it.name }.forEach { appendLine("- `$it`") }
            appendLine()
        }
        appendLine("## Escenarios ejecutados")
        appendLine()
        appendLine("| Escenario | Estado | Documento | Estado documento | Autorizado | Entregado | Mensajes |")
        appendLine("|---|---:|---|---|---:|---:|---|")
        report.scenarioResults.forEach { result ->
            appendLine(
                "| `${result.code}` | ${result.status} | ${result.documentId.orDash()} | ${result.finalDocumentStatus?.name.orDash()} | ${result.authorized} | ${result.delivered} | ${result.messages.joinToString("<br>") { it.escapeMd() }} |"
            )
        }
        appendLine()
        appendLine("## Regla de salida")
        appendLine()
        appendLine("Producción solo debería habilitarse cuando no existan escenarios obligatorios faltantes y todos los escenarios ejecutados estén en `PASSED`.")
    }

    private fun String?.orDash(): String = this ?: "-"
    private fun String.escapeMd(): String = replace("|", "\\|").replace("\n", " ")
}
