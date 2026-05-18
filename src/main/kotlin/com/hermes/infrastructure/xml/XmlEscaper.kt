package com.hermes.infrastructure.xml

internal object XmlEscaper {
    fun text(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    fun attribute(value: String): String = text(value)
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
