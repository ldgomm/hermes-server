package com.hermes.application.electronicinvoicing

/**
 * Validates generated SRI XML against the official XSD selected by schemaVersionCode.
 *
 * This port is intentionally small so infrastructure can validate with JAXP today
 * and be replaced later if SRI-specific schema handling becomes necessary.
 */
interface SriXsdValidator {
    fun validate(xml: ByteArray, schemaVersionCode: String): XsdValidationResult
}
