package com.hermes.application.auth

interface InvitationDelivery {
    fun buildInvitationUrl(rawToken: String): String? = null
    fun deliverInvitation(email: String, rawToken: String, invitationUrl: String?) = Unit
}

interface PasswordResetDelivery {
    fun buildResetUrl(rawToken: String): String? = null
    fun deliverPasswordReset(email: String, rawToken: String, resetUrl: String?) = Unit
}

object NoopInvitationDelivery : InvitationDelivery
object NoopPasswordResetDelivery : PasswordResetDelivery
