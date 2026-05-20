package com.hermes.application.admin.support

interface AdminSupportQueryRepository {
    fun searchAuditLogs(command: SearchAdminAuditLogsCommand): List<AdminAuditLogRecord>
    fun auditTimeline(command: GetAdminAuditTimelineCommand): List<AdminAuditTimelineItem>
    fun diagnostics(command: GetAdminSupportDiagnosticsCommand): AdminSupportDiagnosticsReport
    fun permissions(command: GetAdminSupportPermissionsCommand): AdminSupportPermissionsReport
    fun modules(command: GetAdminSupportModulesCommand): AdminSupportModulesReport
}
