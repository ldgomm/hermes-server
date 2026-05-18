package com.hermes.infrastructure.xml

import com.hermes.application.electronicinvoicing.SriXsdValidator
import com.hermes.application.electronicinvoicing.XsdValidationError
import com.hermes.application.electronicinvoicing.XsdValidationResult
import com.hermes.application.electronicinvoicing.XsdValidationSeverity
import com.hermes.domain.shared.DomainRuleViolation
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import org.xml.sax.ErrorHandler
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException

class JaxpSriXsdValidator(
    private val schemaSource: XsdSchemaSource = ClasspathXsdSchemaSource(),
) : SriXsdValidator {
    private val schemaCache = ConcurrentHashMap<String, Schema>()

    override fun validate(xml: ByteArray, schemaVersionCode: String): XsdValidationResult {
        if (xml.isEmpty()) throw DomainRuleViolation("XML content cannot be empty for XSD validation.")
        val code = schemaVersionCode.trim()
        if (code.isBlank()) throw DomainRuleViolation("XSD schema version code cannot be blank.")

        val collectingErrorHandler = CollectingXsdErrorHandler()
        return try {
            val validator = schemaFor(code).newValidator()
            validator.errorHandler = collectingErrorHandler
            validator.validate(StreamSource(ByteArrayInputStream(xml)))

            if (collectingErrorHandler.errors.isEmpty()) {
                XsdValidationResult.valid(code, warnings = collectingErrorHandler.warnings)
            } else {
                XsdValidationResult.invalid(
                    schemaVersionCode = code,
                    errors = collectingErrorHandler.errors,
                    warnings = collectingErrorHandler.warnings,
                )
            }
        } catch (error: SAXParseException) {
            collectingErrorHandler.toInvalidResult(code, error)
        } catch (error: SAXException) {
            collectingErrorHandler.toInvalidResult(code, error)
        }
    }

    private fun schemaFor(schemaVersionCode: String): Schema = schemaCache.computeIfAbsent(schemaVersionCode) { code ->
        try {
            schemaSource.load(code).use { loaded ->
                val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                factory.newSchema(StreamSource(loaded.inputStream, loaded.systemId))
            }
        } catch (error: SAXException) {
            throw DomainRuleViolation(
                "SRI XSD schema could not be loaded for $code: ${error.message ?: "invalid schema"}."
            )
        }
    }
}

private class CollectingXsdErrorHandler : ErrorHandler {
    val warnings = mutableListOf<XsdValidationError>()
    val errors = mutableListOf<XsdValidationError>()

    override fun warning(exception: SAXParseException) {
        warnings += exception.toValidationError(XsdValidationSeverity.WARNING)
    }

    override fun error(exception: SAXParseException) {
        errors += exception.toValidationError(XsdValidationSeverity.ERROR)
    }

    override fun fatalError(exception: SAXParseException) {
        errors += exception.toValidationError(XsdValidationSeverity.FATAL)
    }

    fun toInvalidResult(schemaVersionCode: String, error: SAXException): XsdValidationResult {
        val collectedErrors = errors.ifEmpty {
            listOf(error.toValidationError())
        }
        return XsdValidationResult.invalid(
            schemaVersionCode = schemaVersionCode,
            errors = collectedErrors,
            warnings = warnings,
        )
    }
}

private fun SAXParseException.toValidationError(severity: XsdValidationSeverity): XsdValidationError = XsdValidationError(
    severity = severity,
    message = message ?: "XML does not satisfy XSD schema.",
    line = lineNumber.takeIf { it > 0 },
    column = columnNumber.takeIf { it > 0 },
    publicId = publicId?.takeIf { it.isNotBlank() },
    systemId = systemId?.takeIf { it.isNotBlank() },
)

private fun SAXException.toValidationError(): XsdValidationError = XsdValidationError(
    severity = XsdValidationSeverity.ERROR,
    message = message ?: "XML does not satisfy XSD schema.",
)
