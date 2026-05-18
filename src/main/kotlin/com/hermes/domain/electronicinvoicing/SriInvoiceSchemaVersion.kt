package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation

enum class SriInvoiceSchemaVersion(
    val version: String,
    val schemaVersionCode: String,
) {
    V2_1_0("2.1.0", "factura_V2.1.0");

    companion object {
        fun fromVersion(version: String): SriInvoiceSchemaVersion {
            val normalized = version.trim()
            return entries.firstOrNull { it.version == normalized }
                ?: throw DomainRuleViolation("Unsupported SRI invoice schema version: $version.")
        }
    }
}
