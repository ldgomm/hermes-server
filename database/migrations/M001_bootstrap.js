const dbName = 'hermes_local';
const target = db.getSiblingDB(dbName);

const migrationId = 'M001_bootstrap';
const alreadyApplied = target.schema_migrations.findOne({_id: migrationId});

if (alreadyApplied) {
    print(`Migration ${migrationId} already applied.`);
    quit(0);
}

print(`Applying migration ${migrationId} on ${dbName}...`);

target.createCollection('schema_migrations');
target.createCollection('health_checks');
target.createCollection('outbox_events');
target.createCollection('audit_logs');

target.health_checks.createIndex({createdAt: -1});
target.outbox_events.createIndex({status: 1, createdAt: 1});
target.audit_logs.createIndex({organizationId: 1, createdAt: -1});

target.schema_migrations.insertOne({
    _id: migrationId,
    name: 'Bootstrap technical collections',
    appliedAt: new Date(),
    version: 1
});

print(`Migration ${migrationId} applied.`);
