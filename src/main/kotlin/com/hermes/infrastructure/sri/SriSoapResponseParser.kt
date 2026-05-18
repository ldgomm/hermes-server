package com.hermes.infrastructure.sri

import com.hermes.application.electronicinvoicing.SriAuthorizationResult
import com.hermes.application.electronicinvoicing.SriReceptionResult
import com.hermes.domain.electronicinvoicing.*
import com.hermes.domain.shared.DomainRuleViolation
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

class SriSoapResponseParser {
    fun parseReception(
        environment: SriEnvironment,
        rawResponseXml: String,
        fallbackAccessKey: SriAccessKey? = null,
        receivedAt: Instant = Instant.now(),
    ): SriReceptionResult {
        val document = parseXml(rawResponseXml)
        val statusText = firstText(document, "estado")
            ?: throw DomainRuleViolation("SRI reception response does not contain estado.")
        val status = SriReceptionStatus.fromSriValue(statusText)
        val accessKey = firstText(document, "claveAcceso")
            ?.takeIf { it.matches(Regex("\\d{49}")) }
            ?.let(::SriAccessKey)
            ?: fallbackAccessKey

        return SriReceptionResult(
            environment = environment,
            status = status,
            accessKey = accessKey,
            messages = parseMessages(document),
            rawResponseXml = rawResponseXml,
            receivedAt = receivedAt,
        )
    }

    fun parseAuthorization(
        environment: SriEnvironment,
        accessKey: SriAccessKey,
        rawResponseXml: String,
        queriedAt: Instant = Instant.now(),
    ): SriAuthorizationResult {
        val document = parseXml(rawResponseXml)
        val authorizationElement = firstElement(document, "autorizacion")
        val statusText = authorizationElement?.directChildText("estado")
            ?: firstText(document, "estado")
            ?: "PPR"
        val status = SriAuthorizationStatus.fromSriValue(statusText)
        val authorizationNumber = authorizationElement?.directChildText("numeroAutorizacion")
            ?: firstText(document, "numeroAutorizacion")
        val authorizedAt = authorizationElement?.directChildText("fechaAutorizacion")
            ?.let(::parseSriInstant)
            ?: firstText(document, "fechaAutorizacion")?.let(::parseSriInstant)
        val authorizedXml = authorizationElement?.directChildText("comprobante")
            ?: firstText(document, "comprobante")

        return SriAuthorizationResult(
            environment = environment,
            status = status,
            accessKey = accessKey,
            authorizationNumber = authorizationNumber,
            authorizedAt = authorizedAt,
            authorizedXml = authorizedXml,
            messages = parseMessages(document),
            rawResponseXml = rawResponseXml,
            queriedAt = queriedAt,
        )
    }

    private fun parseXml(rawXml: String): Document {
        if (rawXml.isBlank()) throw DomainRuleViolation("SRI SOAP response XML cannot be blank.")
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        return runCatching {
            factory.newDocumentBuilder().parse(ByteArrayInputStream(rawXml.toByteArray(Charsets.UTF_8)))
        }.getOrElse { error ->
            throw DomainRuleViolation("SRI SOAP response XML is not well formed: ${error.message}.")
        }
    }

    private fun parseMessages(document: Document): List<SriMessage> =
        elements(document, "mensaje")
            .mapNotNull { node -> node as? Element }
            .filter { element ->
                element.directChildText("identificador") != null ||
                        element.directChildText("informacionAdicional") != null ||
                        element.directChildText("tipo") != null
            }
            .map { element ->
                SriMessage.fromRaw(
                    identifier = element.directChildText("identificador"),
                    message = element.directChildText("mensaje") ?: "Mensaje SRI sin descripción",
                    additionalInfo = element.directChildText("informacionAdicional"),
                    type = element.directChildText("tipo"),
                )
            }

    private fun firstText(document: Document, localName: String): String? =
        elements(document, localName).firstNotNullOfOrNull { node ->
            node.textContent?.trim()?.takeIf { it.isNotBlank() }
        }

    private fun firstElement(document: Document, localName: String): Element? =
        elements(document, localName).firstNotNullOfOrNull { it as? Element }

    private fun elements(document: Document, localName: String): List<Node> {
        val result = mutableListOf<Node>()
        fun walk(node: Node) {
            if (node.matchesLocalName(localName)) result += node
            val children = node.childNodes
            for (index in 0 until children.length) walk(children.item(index))
        }
        walk(document.documentElement)
        return result
    }

    private fun Node.matchesLocalName(expected: String): Boolean {
        val actual = localName ?: nodeName.substringAfter(':')
        return actual == expected
    }

    private fun Element.directChildText(localName: String): String? {
        val children = childNodes
        for (index in 0 until children.length) {
            val child = children.item(index)
            if (child.nodeType == Node.ELEMENT_NODE && child.matchesLocalName(localName)) {
                return child.textContent?.trim()?.takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    private fun parseSriInstant(value: String): Instant? {
        val normalized = value.trim().takeIf { it.isNotBlank() } ?: return null
        return runCatching { OffsetDateTime.parse(normalized).toInstant() }
            .recoverCatching {
                LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(ZoneId.systemDefault())
                    .toInstant()
            }
            .recoverCatching {
                LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
                    .atZone(ZoneId.systemDefault()).toInstant()
            }
            .getOrNull()
    }
}
