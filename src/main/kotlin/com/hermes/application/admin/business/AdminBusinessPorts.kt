package com.hermes.application.admin.business

interface AdminBusinessRepository {
    fun findBusiness(organizationId: String): AdminBusinessProfile?
    fun listActivities(organizationId: String): List<AdminBusinessActivitySummary>
    fun listBranches(organizationId: String): List<AdminBusinessBranchSummary>
    fun listEmissionPoints(organizationId: String): List<AdminBusinessEmissionPointSummary>
    fun hasTaxSettings(organizationId: String): Boolean
    fun hasSriSettings(organizationId: String): Boolean
    fun hasActiveOwnerOrAdminMembership(organizationId: String): Boolean
}
