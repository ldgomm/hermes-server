package com.hermes.domain.payment

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class Receivable private constructor(
    val id: String,
    val organizationId: String,
    val saleId: String,
    val customerId: String?,
    val totalDue: Money,
    val paidAmount: Money,
    val dueAt: Instant?,
    val isVoided: Boolean,
    val isWrittenOff: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {

    init {
        if (id.isBlank()) {
            throw DomainRuleViolation("Receivable id cannot be blank.")
        }

        if (organizationId.isBlank()) {
            throw DomainRuleViolation("Receivable organization id cannot be blank.")
        }

        if (saleId.isBlank()) {
            throw DomainRuleViolation("Receivable sale id cannot be blank.")
        }

        if (paidAmount > totalDue) {
            throw DomainRuleViolation("Receivable paid amount cannot be greater than total due.")
        }

        if (isVoided && isWrittenOff) {
            throw DomainRuleViolation("Receivable cannot be voided and written off at the same time.")
        }
    }

    fun status(now: Instant): CollectionStatus {
        return CollectionStatusResolver.resolve(
            totalDue = totalDue,
            paidAmount = paidAmount,
            dueAt = dueAt,
            now = now,
            isVoided = isVoided,
            isWrittenOff = isWrittenOff
        )
    }

    fun registerCollection(
        amount: Money,
        collectedAt: Instant
    ): Receivable {
        if (isVoided) {
            throw DomainRuleViolation("Cannot collect a voided receivable.")
        }

        if (isWrittenOff) {
            throw DomainRuleViolation("Cannot collect a written-off receivable.")
        }

        if (amount.amount.signum() <= 0) {
            throw DomainRuleViolation("Collection amount must be greater than zero.")
        }

        val newPaidAmount = paidAmount + amount

        if (newPaidAmount > totalDue) {
            throw DomainRuleViolation("Collection amount cannot exceed receivable balance.")
        }

        return copy(
            paidAmount = newPaidAmount,
            updatedAt = collectedAt
        )
    }

    fun writeOff(writtenOffAt: Instant): Receivable {
        if (paidAmount > Money.zero(totalDue.currency)) {
            throw DomainRuleViolation("Partially collected receivable cannot be written off without adjustment.")
        }

        if (isVoided) {
            throw DomainRuleViolation("Voided receivable cannot be written off.")
        }

        if (isWrittenOff) {
            throw DomainRuleViolation("Receivable is already written off.")
        }

        return copy(
            isWrittenOff = true,
            updatedAt = writtenOffAt
        )
    }

    fun void(voidedAt: Instant): Receivable {
        if (paidAmount > Money.zero(totalDue.currency)) {
            throw DomainRuleViolation("Collected receivable cannot be voided without reversal.")
        }

        if (isWrittenOff) {
            throw DomainRuleViolation("Written-off receivable cannot be voided.")
        }

        if (isVoided) {
            throw DomainRuleViolation("Receivable is already voided.")
        }

        return copy(
            isVoided = true,
            updatedAt = voidedAt
        )
    }

    companion object {
        fun createForSale(
            id: String,
            organizationId: String,
            saleId: String,
            customerId: String?,
            totalDue: Money,
            dueAt: Instant?,
            createdAt: Instant
        ): Receivable {
            return Receivable(
                id = id,
                organizationId = organizationId,
                saleId = saleId,
                customerId = customerId?.trim()?.takeIf { it.isNotBlank() },
                totalDue = totalDue,
                paidAmount = Money.zero(totalDue.currency),
                dueAt = dueAt,
                isVoided = false,
                isWrittenOff = false,
                createdAt = createdAt,
                updatedAt = createdAt
            )
        }
    }
}
