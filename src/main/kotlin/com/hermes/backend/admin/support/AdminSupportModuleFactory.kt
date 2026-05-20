package com.hermes.backend.admin.support

import com.hermes.application.admin.support.GetAdminAuditTimelineUseCase
import com.hermes.application.admin.support.GetAdminSupportDiagnosticsUseCase
import com.hermes.application.admin.support.GetAdminSupportModulesUseCase
import com.hermes.application.admin.support.GetAdminSupportPermissionsUseCase
import com.hermes.application.admin.support.SearchAdminAuditLogsUseCase
import com.hermes.infrastructure.mongo.admin.support.MongoAdminSupportQueryRepository
import com.mongodb.client.MongoDatabase

object AdminSupportModuleFactory {
    fun fromMongo(database: MongoDatabase): AdminSupportModule {
        val queryRepository = MongoAdminSupportQueryRepository(database)
        return AdminSupportModule(
            searchAuditLogsUseCase = SearchAdminAuditLogsUseCase(queryRepository),
            auditTimelineUseCase = GetAdminAuditTimelineUseCase(queryRepository),
            supportDiagnosticsUseCase = GetAdminSupportDiagnosticsUseCase(queryRepository),
            supportPermissionsUseCase = GetAdminSupportPermissionsUseCase(queryRepository),
            supportModulesUseCase = GetAdminSupportModulesUseCase(queryRepository),
        )
    }
}
