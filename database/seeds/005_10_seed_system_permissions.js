// Fase 5.10 — Seed de permisos del sistema.
// Idempotente: crea índice único por code y hace upsert por cada permiso.
// Este script es seguro para local/dev/staging/production siempre que se ejecute
// dentro del pipeline de migraciones controlado.

db.permissions.createIndex(
  { code: 1 },
  { unique: true, name: "ux_permissions_code" }
);

const now = new Date();

const permissions = [
  { code: "credentials.users.view", name: "View users", category: "CREDENTIALS", scope: "ORGANIZATION", riskLevel: "MEDIUM", status: "ACTIVE" },
  { code: "credentials.users.create", name: "Create users", category: "CREDENTIALS", scope: "ORGANIZATION", riskLevel: "CRITICAL", status: "ACTIVE", requiresAudit: true },
  { code: "credentials.users.invite", name: "Invite users", category: "CREDENTIALS", scope: "ORGANIZATION", riskLevel: "CRITICAL", status: "ACTIVE", requiresAudit: true },
  { code: "credentials.users.reset_password", name: "Reset user password", category: "CREDENTIALS", scope: "ORGANIZATION", riskLevel: "CRITICAL", status: "ACTIVE", requiresAudit: true, requiresReason: true, requiresStepUp: true },
  { code: "credentials.users.block", name: "Block users", category: "CREDENTIALS", scope: "ORGANIZATION", riskLevel: "CRITICAL", status: "ACTIVE", requiresAudit: true, requiresReason: true, requiresStepUp: true },
  { code: "credentials.users.unblock", name: "Unblock users", category: "CREDENTIALS", scope: "ORGANIZATION", riskLevel: "CRITICAL", status: "ACTIVE", requiresAudit: true, requiresReason: true, requiresStepUp: true },
  { code: "credentials.sessions.revoke", name: "Revoke sessions", category: "CREDENTIALS", scope: "ORGANIZATION", riskLevel: "CRITICAL", status: "ACTIVE", requiresAudit: true, requiresReason: true },

  { code: "credentials.roles.view", name: "View roles", category: "CREDENTIALS", scope: "ORGANIZATION", riskLevel: "MEDIUM", status: "ACTIVE" },
  { code: "credentials.roles.assign", name: "Assign roles", category: "CREDENTIALS", scope: "ORGANIZATION", riskLevel: "CRITICAL", status: "ACTIVE", requiresAudit: true, requiresReason: true, requiresStepUp: true },
  { code: "credentials.roles.manage", name: "Manage roles", category: "CREDENTIALS", scope: "ORGANIZATION", riskLevel: "CRITICAL", status: "ACTIVE", requiresAudit: true, requiresReason: true, requiresStepUp: true },

  { code: "organization.view", name: "View organization", category: "ORGANIZATION", scope: "ORGANIZATION", riskLevel: "LOW", status: "ACTIVE" },
  { code: "organization.update", name: "Update organization", category: "ORGANIZATION", scope: "ORGANIZATION", riskLevel: "CRITICAL", status: "ACTIVE", requiresAudit: true },
  { code: "organization.members.view", name: "View organization members", category: "ORGANIZATION", scope: "ORGANIZATION", riskLevel: "MEDIUM", status: "ACTIVE" },
  { code: "organization.members.manage", name: "Manage organization members", category: "ORGANIZATION", scope: "ORGANIZATION", riskLevel: "CRITICAL", status: "ACTIVE", requiresAudit: true, requiresReason: true },

  { code: "settings.branches.view", name: "View branch settings", category: "SETTINGS", scope: "ORGANIZATION", riskLevel: "LOW", status: "ACTIVE" },
  { code: "settings.branches.manage", name: "Manage branch settings", category: "SETTINGS", scope: "ORGANIZATION", riskLevel: "HIGH", status: "ACTIVE", requiresAudit: true },
  { code: "settings.emission_points.view", name: "View emission points", category: "SETTINGS", scope: "ORGANIZATION", riskLevel: "MEDIUM", status: "ACTIVE" },
  { code: "settings.emission_points.manage", name: "Manage emission points", category: "SETTINGS", scope: "ORGANIZATION", riskLevel: "CRITICAL", status: "ACTIVE", requiresAudit: true, requiresReason: true, requiresStepUp: true },

  { code: "sales.view", name: "View sales", category: "SALES", scope: "ORGANIZATION", riskLevel: "LOW", status: "ACTIVE" },
  { code: "sales.create", name: "Create sales", category: "SALES", scope: "ORGANIZATION", riskLevel: "MEDIUM", status: "ACTIVE" },
  { code: "sales.cancel", name: "Cancel sales", category: "SALES", scope: "ORGANIZATION", riskLevel: "HIGH", status: "ACTIVE", requiresAudit: true, requiresReason: true },

  { code: "payments.view", name: "View payments", category: "PAYMENTS", scope: "ORGANIZATION", riskLevel: "LOW", status: "ACTIVE" },
  { code: "payments.collect", name: "Collect payments", category: "PAYMENTS", scope: "ORGANIZATION", riskLevel: "HIGH", status: "ACTIVE", requiresAudit: true },

  { code: "cash.view", name: "View cash", category: "CASH", scope: "ORGANIZATION", riskLevel: "LOW", status: "ACTIVE" },
  { code: "cash.open", name: "Open cash legacy", category: "CASH", scope: "ORGANIZATION", riskLevel: "HIGH", status: "ACTIVE", requiresAudit: true },
  { code: "cash.close", name: "Close cash legacy", category: "CASH", scope: "ORGANIZATION", riskLevel: "HIGH", status: "ACTIVE", requiresAudit: true },
  { code: "cash.session.open", name: "Open cash session", category: "CASH", scope: "ORGANIZATION", riskLevel: "HIGH", status: "ACTIVE", requiresAudit: true },
  { code: "cash.session.close", name: "Close cash session", category: "CASH", scope: "ORGANIZATION", riskLevel: "CRITICAL", status: "ACTIVE", requiresAudit: true, requiresReason: true },

  { code: "documents.view", name: "View documents", category: "DOCUMENTS", scope: "ORGANIZATION", riskLevel: "LOW", status: "ACTIVE" },
  { code: "documents.issue_electronic_invoice", name: "Issue electronic invoice", category: "DOCUMENTS", scope: "ORGANIZATION", riskLevel: "CRITICAL", status: "ACTIVE", requiresAudit: true, requiresReason: true, requiresStepUp: true },
  { code: "documents.download_pdf", name: "Download document PDF", category: "DOCUMENTS", scope: "ORGANIZATION", riskLevel: "LOW", status: "ACTIVE" },
  { code: "documents.download_xml", name: "Download document XML", category: "DOCUMENTS", scope: "ORGANIZATION", riskLevel: "MEDIUM", status: "ACTIVE" },

  { code: "signature.view_metadata", name: "View signature metadata", category: "SIGNATURE", scope: "ORGANIZATION", riskLevel: "MEDIUM", status: "ACTIVE" },
  { code: "signature.upload", name: "Upload signature", category: "SIGNATURE", scope: "ORGANIZATION", riskLevel: "CRITICAL", status: "ACTIVE", requiresAudit: true, requiresReason: true, requiresStepUp: true },
  { code: "signature.replace", name: "Replace signature", category: "SIGNATURE", scope: "ORGANIZATION", riskLevel: "CRITICAL", status: "ACTIVE", requiresAudit: true, requiresReason: true, requiresStepUp: true },
  { code: "signature.revoke", name: "Revoke signature", category: "SIGNATURE", scope: "ORGANIZATION", riskLevel: "CRITICAL", status: "ACTIVE", requiresAudit: true, requiresReason: true, requiresStepUp: true },
  { code: "signature.test", name: "Test signature", category: "SIGNATURE", scope: "ORGANIZATION", riskLevel: "HIGH", status: "ACTIVE", requiresAudit: true },
  { code: "signature.use_for_invoicing", name: "Use signature for invoicing", category: "SIGNATURE", scope: "ORGANIZATION", riskLevel: "CRITICAL", status: "ACTIVE", requiresAudit: true, requiresReason: true, requiresStepUp: true },
  { code: "signature.view_audit", name: "View signature audit", category: "SIGNATURE", scope: "ORGANIZATION", riskLevel: "HIGH", status: "ACTIVE" },

  { code: "audit.view", name: "View audit", category: "AUDIT", scope: "ORGANIZATION", riskLevel: "HIGH", status: "ACTIVE" }
];

for (const permission of permissions) {
  db.permissions.updateOne(
    { code: permission.code },
    {
      $setOnInsert: {
        code: permission.code,
        createdAt: now,
        createdBy: "system",
        schemaVersion: 1
      },
      $set: {
        ...permission,
        description: permission.description || permission.name,
        systemManaged: true,
        updatedAt: now,
        updatedBy: "system",
        seedVersion: "2026.05.phase5.10"
      }
    },
    { upsert: true }
  );
}
