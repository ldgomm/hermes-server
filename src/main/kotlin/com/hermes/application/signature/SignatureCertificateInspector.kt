package com.hermes.application.signature

import com.hermes.domain.signature.SignatureCertificateMetadata

interface SignatureCertificateInspector {
    fun inspectPkcs12(content: ByteArray, password: CharArray, fileName: String): SignatureCertificateMetadata
}
