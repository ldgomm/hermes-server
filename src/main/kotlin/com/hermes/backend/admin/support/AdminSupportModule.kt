package com.hermes.backend.admin.support

import com.hermes.application.admin.support.GetAdminAuditTimelineUseCase
import com.hermes.application.admin.support.GetAdminSupportDiagnosticsUseCase
import com.hermes.application.admin.support.GetAdminSupportModulesUseCase
import com.hermes.application.admin.support.GetAdminSupportPermissionsUseCase
import com.hermes.application.admin.support.SearchAdminAuditLogsUseCase

data class AdminSupportModule(
    val searchAuditLogsUseCase: SearchAdminAuditLogsUseCase,
    val auditTimelineUseCase: GetAdminAuditTimelineUseCase,
    val supportDiagnosticsUseCase: GetAdminSupportDiagnosticsUseCase,
    val supportPermissionsUseCase: GetAdminSupportPermissionsUseCase,
    val supportModulesUseCase: GetAdminSupportModulesUseCase,
)
