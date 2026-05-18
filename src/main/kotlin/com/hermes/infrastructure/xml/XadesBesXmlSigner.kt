package com.hermes.infrastructure.xml

import com.hermes.application.electronicinvoicing.SignXmlCommand
import com.hermes.application.electronicinvoicing.SignedXml
import com.hermes.application.electronicinvoicing.XmlSigner
import com.hermes.application.electronicinvoicing.XmlSigningKeyMaterial
import com.hermes.domain.shared.DomainRuleViolation
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.time.format.DateTimeFormatter
import java.util.*
import javax.xml.XMLConstants
import javax.xml.crypto.dom.DOMStructure
import javax.xml.crypto.dsig.*
import javax.xml.crypto.dsig.dom.DOMSignContext
import javax.xml.crypto.dsig.keyinfo.KeyInfo
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec
import javax.xml.crypto.dsig.spec.TransformParameterSpec
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * JDK-only XML signer for the SRI flow.
 *
 * It generates an enveloped XML Digital Signature and embeds the XAdES-BES
 * qualifying properties object expected by the Ecuadorian electronic invoice flow.
 * Homologation must still verify the exact canonicalization/signature profile required
 * by the current SRI environment before production rollout.
 */
class XadesBesXmlSigner : XmlSigner {
    override fun sign(command: SignXmlCommand, keyMaterial: XmlSigningKeyMaterial): SignedXml {
        val document = parseXml(command.xml)
        val root = document.documentElement ?: throw DomainRuleViolation("XML document does not have root element.")
        val signatureNodeName = root.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature")
        if (signatureNodeName.length > 0) {
            throw DomainRuleViolation("XML document is already signed.")
        }

        ensureComprobanteId(root)

        val signatureFactory = XMLSignatureFactory.getInstance("DOM")
        val signatureId = "Signature-${safeId(command.signatureId)}-${command.signedAt.toEpochMilli()}"
        val signedPropertiesId = "SignedProperties-$signatureId"

        val transforms = listOf(
            signatureFactory.newTransform(Transform.ENVELOPED, null as TransformParameterSpec?),
            signatureFactory.newTransform(CanonicalizationMethod.INCLUSIVE, null as TransformParameterSpec?),
        )

        val documentReference = signatureFactory.newReference(
            "#comprobante",
            signatureFactory.newDigestMethod(DigestMethod.SHA1, null),
            transforms,
            null,
            "Reference-$signatureId",
        )

        val signedInfo = signatureFactory.newSignedInfo(
            signatureFactory.newCanonicalizationMethod(
                CanonicalizationMethod.INCLUSIVE,
                null as C14NMethodParameterSpec?
            ),
            signatureFactory.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
            listOf(documentReference),
        )

        val keyInfo = createKeyInfo(signatureFactory, keyMaterial)
        val xadesObject = signatureFactory.newXMLObject(
            listOf(
                DOMStructure(
                    createQualifyingProperties(
                        document,
                        keyMaterial,
                        signatureId,
                        signedPropertiesId,
                        command
                    )
                )
            ),
            "Object-$signatureId",
            null,
            null,
        )

        val xmlSignature = signatureFactory.newXMLSignature(
            signedInfo,
            keyInfo,
            listOf(xadesObject),
            signatureId,
            null,
        )

        val context = DOMSignContext(keyMaterial.privateKey, root)
        context.defaultNamespacePrefix = "ds"
        xmlSignature.sign(context)

        val signed = serialize(document)
        val certificateFingerprint = sha256Hex(keyMaterial.certificate.encoded)
        return SignedXml(
            signatureId = command.signatureId,
            signedXml = signed,
            signedXmlSha256 = sha256Hex(signed),
            signedAt = command.signedAt,
            certificateSerialNumber = keyMaterial.certificate.serialNumber.toString(),
            certificateFingerprintSha256 = certificateFingerprint,
            signatureAlgorithm = SignatureMethod.RSA_SHA1,
            digestAlgorithm = DigestMethod.SHA1,
            xadesBesObjectIncluded = signed.toString(Charsets.UTF_8).contains("QualifyingProperties"),
        )
    }

    private fun parseXml(xml: ByteArray): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        runCatching { factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
        runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { factory.setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        runCatching { factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "") }
        runCatching { factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "") }

        return try {
            factory.newDocumentBuilder().parse(ByteArrayInputStream(xml))
        } catch (error: Exception) {
            throw DomainRuleViolation("XML cannot be parsed for signing: ${error.message}")
        }
    }

    private fun ensureComprobanteId(root: Element) {
        if (root.localName != "factura" && root.tagName != "factura") {
            throw DomainRuleViolation("Only factura XML can be signed by this signer.")
        }
        val id = root.getAttribute("id").trim()
        if (id.isBlank()) {
            root.setAttribute("id", "comprobante")
        }
        if (root.getAttribute("id") != "comprobante") {
            throw DomainRuleViolation("Factura XML root id must be comprobante before signing.")
        }
        root.setIdAttribute("id", true)
    }

    private fun createKeyInfo(
        signatureFactory: XMLSignatureFactory,
        keyMaterial: XmlSigningKeyMaterial,
    ): KeyInfo {
        val keyInfoFactory = signatureFactory.keyInfoFactory
        val x509Data = keyInfoFactory.newX509Data(
            listOf(
                keyMaterial.certificate.subjectX500Principal.name,
                keyMaterial.certificate,
            )
        )
        return keyInfoFactory.newKeyInfo(listOf(x509Data))
    }

    private fun createQualifyingProperties(
        document: Document,
        keyMaterial: XmlSigningKeyMaterial,
        signatureId: String,
        signedPropertiesId: String,
        command: SignXmlCommand,
    ): Element {
        val qualifyingProperties = document.createElementNS(XADES_NS, "etsi:QualifyingProperties")
        qualifyingProperties.setAttribute("Target", "#$signatureId")

        val signedProperties = document.createElementNS(XADES_NS, "etsi:SignedProperties")
        signedProperties.setAttribute("Id", signedPropertiesId)

        val signedSignatureProperties = document.createElementNS(XADES_NS, "etsi:SignedSignatureProperties")
        val signingTime = document.createElementNS(XADES_NS, "etsi:SigningTime")
        signingTime.textContent = DateTimeFormatter.ISO_INSTANT.format(command.signedAt)

        val signingCertificate = document.createElementNS(XADES_NS, "etsi:SigningCertificate")
        val cert = document.createElementNS(XADES_NS, "etsi:Cert")

        val certDigest = document.createElementNS(XADES_NS, "etsi:CertDigest")
        val digestMethod = document.createElementNS(XMLSignature.XMLNS, "ds:DigestMethod")
        digestMethod.setAttribute("Algorithm", DigestMethod.SHA1)
        val digestValue = document.createElementNS(XMLSignature.XMLNS, "ds:DigestValue")
        digestValue.textContent = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-1").digest(keyMaterial.certificate.encoded)
        )
        certDigest.appendChild(digestMethod)
        certDigest.appendChild(digestValue)

        val issuerSerial = document.createElementNS(XADES_NS, "etsi:IssuerSerial")
        val issuerName = document.createElementNS(XMLSignature.XMLNS, "ds:X509IssuerName")
        issuerName.textContent = keyMaterial.certificate.issuerX500Principal.name
        val serialNumber = document.createElementNS(XMLSignature.XMLNS, "ds:X509SerialNumber")
        serialNumber.textContent = keyMaterial.certificate.serialNumber.toString()
        issuerSerial.appendChild(issuerName)
        issuerSerial.appendChild(serialNumber)

        cert.appendChild(certDigest)
        cert.appendChild(issuerSerial)
        signingCertificate.appendChild(cert)

        signedSignatureProperties.appendChild(signingTime)
        signedSignatureProperties.appendChild(signingCertificate)
        signedProperties.appendChild(signedSignatureProperties)
        qualifyingProperties.appendChild(signedProperties)

        return qualifyingProperties
    }

    private fun serialize(document: Document): ByteArray {
        val transformerFactory = TransformerFactory.newInstance()
        runCatching { transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "") }
        runCatching { transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "") }
        val transformer = transformerFactory.newTransformer()
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no")
        transformer.setOutputProperty(OutputKeys.INDENT, "no")
        val output = ByteArrayOutputStream()
        transformer.transform(DOMSource(document), StreamResult(output))
        return output.toByteArray()
    }

    private fun sha256Hex(content: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(content)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private fun safeId(value: String): String =
        value.trim().replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "sig" }

    private companion object {
        const val XADES_NS = "http://uri.etsi.org/01903/v1.3.2#"
    }
}
