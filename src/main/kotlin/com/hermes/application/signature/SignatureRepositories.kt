package com.hermes.application.signature

import com.hermes.domain.signature.ElectronicSignature

interface ElectronicSignatureRepository {
    fun create(signature: ElectronicSignature)
    fun update(signature: ElectronicSignature)
    fun findById(id: String): ElectronicSignature?
    fun findActiveByOrganizationId(organizationId: String): ElectronicSignature?
    fun findByOrganizationId(organizationId: String): List<ElectronicSignature>
}

interface SignatureSecretStore {
    fun storeEncryptedSignature(
        organizationId: String,
        signatureId: String,
        fileName: String,
        content: ByteArray,
    ): String

    fun storePasswordSecret(
        organizationId: String,
        signatureId: String,
        password: CharArray,
    ): String
}
