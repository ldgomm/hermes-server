package com.hermes.infrastructure.xml

import com.hermes.domain.shared.DomainRuleViolation
import java.io.InputStream
import java.net.URL

interface XsdSchemaSource {
    fun load(schemaVersionCode: String): LoadedXsdSchema
}

data class LoadedXsdSchema(
    val schemaVersionCode: String,
    val systemId: String,
    val inputStream: InputStream,
) : AutoCloseable {
    override fun close() {
        inputStream.close()
    }
}

class ClasspathXsdSchemaSource(
    private val resourcesBySchemaVersionCode: Map<String, String> = SriXsdResourceCatalog.officialInvoiceSchemas,
    private val classLoader: ClassLoader = Thread.currentThread().contextClassLoader
        ?: ClasspathXsdSchemaSource::class.java.classLoader,
) : XsdSchemaSource {

    override fun load(schemaVersionCode: String): LoadedXsdSchema {
        val code = schemaVersionCode.trim()
        if (code.isBlank()) {
            throw DomainRuleViolation("XSD schema version code cannot be blank.")
        }

        val resourcePath = resourcesBySchemaVersionCode[code]
            ?: throw DomainRuleViolation("Unsupported SRI XSD schema version code: $schemaVersionCode.")

        val url = findResource(resourcePath) ?: throw DomainRuleViolation(
            "SRI XSD resource not found for $code at classpath:$resourcePath. " + "Place it under src/main/resources or src/test/resources using the same path."
        )

        return LoadedXsdSchema(
            schemaVersionCode = code,
            systemId = url.toExternalForm(),
            inputStream = url.openStream(),
        )
    }

    private fun findResource(resourcePath: String): URL? {
        val normalized = resourcePath.removePrefix("/")

        return classLoader.getResource(normalized) ?: Thread.currentThread().contextClassLoader?.getResource(normalized)
        ?: ClasspathXsdSchemaSource::class.java.classLoader?.getResource(normalized) ?: ClassLoader.getSystemResource(
            normalized
        )
    }
}