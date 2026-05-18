package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.SriInvoiceSchemaVersion
import java.security.MessageDigest

class GeneratedXml private constructor(
    val schemaVersion: SriInvoiceSchemaVersion,
    val xml: String,
    val encoding: String = "UTF-8",
) {
    val bytes: ByteArray get() = xml.toByteArray(Charsets.UTF_8)
    val sha256: String get() = bytes.sha256Hex()

    init {
        require(xml.isNotBlank()) { "Generated XML cannot be blank." }
    }

    companion object {
        fun of(schemaVersion: SriInvoiceSchemaVersion, xml: String): GeneratedXml =
            GeneratedXml(schemaVersion = schemaVersion, xml = xml)
    }
}

private fun ByteArray.sha256Hex(): String = MessageDigest.getInstance("SHA-256")
    .digest(this)
    .joinToString(separator = "") { byte -> "%02x".format(byte) }
