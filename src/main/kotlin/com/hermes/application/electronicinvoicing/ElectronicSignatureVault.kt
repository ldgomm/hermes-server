package com.hermes.application.electronicinvoicing

import com.hermes.application.signature.ElectronicSignatureRepository
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.signature.ElectronicSignature
import java.time.Instant
import java.util.*

interface ElectronicSignatureSecretReader {
    fun readSignatureContent(encryptedFileObjectKey: String): ByteArray
    fun readPassword(encryptedPasswordRef: String): CharArray
}

data class ElectronicSignatureSigningMaterial(
    val signature: ElectronicSignature,
    val keyMaterial: XmlSigningKeyMaterial,
)

interface ElectronicSignatureVault {
    fun loadActiveForSigning(organizationId: String, now: Instant): ElectronicSignatureSigningMaterial
    fun loadForSigning(organizationId: String, signatureId: String, now: Instant): ElectronicSignatureSigningMaterial
}

class DefaultElectronicSignatureVault(
    private val signatureRepository: ElectronicSignatureRepository,
    private val secretReader: ElectronicSignatureSecretReader,
    private val keyMaterialLoader: XmlSigningKeyMaterialLoader,
) : ElectronicSignatureVault {

    override fun loadActiveForSigning(organizationId: String, now: Instant): ElectronicSignatureSigningMaterial {
        val normalizedOrganizationId = organizationId.trim()
        if (normalizedOrganizationId.isBlank()) {
            throw DomainRuleViolation("Organization id is required to load active electronic signature.")
        }

        val signature = signatureRepository.findActiveByOrganizationId(normalizedOrganizationId)
            ?: throw DomainRuleViolation("Organization does not have an active electronic signature.")

        return load(signature, normalizedOrganizationId, now)
    }

    override fun loadForSigning(
        organizationId: String,
        signatureId: String,
        now: Instant,
    ): ElectronicSignatureSigningMaterial {
        val normalizedOrganizationId = organizationId.trim()
        val normalizedSignatureId = signatureId.trim()
        if (normalizedOrganizationId.isBlank()) throw DomainRuleViolation("Organization id is required.")
        if (normalizedSignatureId.isBlank()) throw DomainRuleViolation("Electronic signature id is required.")

        val signature = signatureRepository.findById(normalizedSignatureId)
            ?: throw DomainRuleViolation("Electronic signature does not exist.")
        if (signature.organizationId != normalizedOrganizationId) {
            throw DomainRuleViolation("Electronic signature does not belong to requested organization.")
        }

        return load(signature, normalizedOrganizationId, now)
    }

    private fun load(
        signature: ElectronicSignature,
        organizationId: String,
        now: Instant,
    ): ElectronicSignatureSigningMaterial {
        if (signature.organizationId != organizationId) {
            throw DomainRuleViolation("Electronic signature organization mismatch.")
        }

        signature.assertUsable(now)

        val content = secretReader.readSignatureContent(signature.storageKey)
        val password = secretReader.readPassword(signature.passwordSecretRef)
        try {
            val keyMaterial = keyMaterialLoader.loadPkcs12(content, password)
            return ElectronicSignatureSigningMaterial(signature = signature, keyMaterial = keyMaterial)
        } finally {
            Arrays.fill(password, '\u0000')
        }
    }
}
