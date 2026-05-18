package com.hermes.infrastructure.xml

import com.hermes.domain.electronicinvoicing.SriInvoiceSchemaVersion

/**
 * Classpath resource map for official SRI XSD files.
 *
 * Put the official file here:
 * src/main/resources/sri/xsd/factura_V2.1.0.xsd
 */
object SriXsdResourceCatalog {
    val officialInvoiceSchemas: Map<String, String> = SriInvoiceSchemaVersion.entries.associate { version ->
        version.schemaVersionCode to "sri/xsd/${version.schemaVersionCode}.xsd"
    }
}
