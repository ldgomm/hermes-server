# Fase 13B.7 — Cierre técnico integral de Users/Roles/Invitations Admin API

## 1. Contexto

La Fase 13B deja cerrada la administración de acceso para Admin iOS/Web Admin:

```text
Users
Roles
Permissions
Invitations
Temporary users
Password reset admin
Block / unblock
Session revocation
```

La decisión importante de cierre es que este bloque ya no debe crecer lateralmente. A partir de aquí, cualquier nueva mejora de acceso debe entrar como hardening puntual, no como expansión de alcance.

---

## 2. Resultado del cierre

### Endpoints cerrados

```http
GET  /api/v1/admin/users
POST /api/v1/admin/users/temporary
GET  /api/v1/admin/users/{userId}
PUT  /api/v1/admin/users/{userId}
POST /api/v1/admin/users/{userId}/block
POST /api/v1/admin/users/{userId}/unblock
POST /api/v1/admin/users/{userId}/reset-password
POST /api/v1/admin/users/{userId}/revoke-sessions

POST /api/v1/admin/invitations
GET  /api/v1/admin/invitations
GET  /api/v1/admin/invitations/{invitationId}
POST /api/v1/admin/invitations/{invitationId}/resend
POST /api/v1/admin/invitations/{invitationId}/revoke

GET  /api/v1/admin/roles
POST /api/v1/admin/roles
GET  /api/v1/admin/roles/{roleId}
PUT  /api/v1/admin/roles/{roleId}
POST /api/v1/admin/roles/{roleId}/activate
POST /api/v1/admin/roles/{roleId}/deactivate

GET  /api/v1/admin/permissions
```

---

## 3. Artefactos agregados en 13B.7

### Código

```text
src/main/kotlin/com/hermes/application/admin/access/AdminAccessApiContract.kt
src/main/kotlin/com/hermes/application/admin/access/AdminAccessPhase13BClosure.kt
```

### Tests

```text
src/test/kotlin/com/hermes/application/admin/access/AdminAccessPhase13BClosureContractTest.kt
```

### Documentación

```text
docs/admin-access/13B_cierre_tecnico_integral.md
docs/admin-access/13B_smoke_test_manual.md
```

---

## 4. Reglas de seguridad cerradas

### Urgente / obligatorio

- Todas las rutas multiempresa dependen de organización activa.
- Toda mutación requiere `reason`.
- Toda mutación queda marcada como auditable en el contrato ejecutable.
- Los roles custom no aceptan permisos desconocidos.
- Los roles custom no aceptan wildcard `*`.
- No se mutan roles `systemRole`, críticos o no editables.
- No se puede bloquear al propio actor.
- No se puede resetear la contraseña propia desde flujo admin.
- No se puede dejar el negocio sin administrador activo.
- No se puede dejar el negocio sin gestor activo de roles.

### Importante

- Las invitaciones usan el flujo existente de Auth para no duplicar lógica sensible.
- Reset password admin devuelve contraseña temporal solo como respuesta operacional inmediata.
- Revoke sessions afecta sesiones y refresh tokens activos.
- El catálogo de permisos se expone como DTO público, no como entidad interna.

### Opcional / posterior

- Step-up authentication para cambios críticos.
- Email real de reenvío de invitación si todavía está en modo placeholder.
- Auditoría persistida global unificada si aún queda como logger noop/fake en tests.
- Rate limiting por actor/IP para reset password, resend invitation y revoke sessions.

---

## 5. Matriz de permisos

| Superficie | Acción | Permiso recomendado |
|---|---|---|
| Users | List/detail | `credentials.users.view` |
| Users | Create temporary | `credentials.users.create` |
| Users | Update profile/roles | `credentials.users.create` |
| Users | Block | `credentials.users.block` |
| Users | Unblock | `credentials.users.unblock` |
| Users | Reset password | `credentials.users.reset_password` |
| Users | Revoke sessions | `credentials.sessions.revoke` |
| Invitations | Create/resend/revoke | `credentials.users.invite` |
| Invitations | List/detail | `credentials.users.view` |
| Roles | List/detail | `credentials.roles.view` |
| Roles | Create/update/activate/deactivate | `credentials.roles.manage` |
| Permissions | List | `credentials.roles.view` |

---

## 6. Criterio de salida

13B puede darse por cerrada cuando:

```bash
./gradlew clean test
```

queda verde después de aplicar este paquete.

Además, antes de abrir Admin iOS para esta sección, conviene hacer un smoke test manual de:

1. crear usuario temporal;
2. listar usuario;
3. actualizar roles;
4. bloquear usuario;
5. desbloquear usuario;
6. resetear contraseña;
7. revocar sesiones;
8. crear invitación;
9. reenviar invitación;
10. revocar invitación;
11. crear rol;
12. actualizar rol;
13. desactivar rol;
14. activar rol;
15. listar permisos.

---

## 7. Decisión recomendada

Dar por cerrada **Fase 13B** si los tests están verdes y seguir con:

```text
13C — Catalog Admin API
```

No conviene seguir agrandando 13B. Ya tiene suficiente superficie sensible y debe mantenerse estable para que Admin iOS pueda consumirla sin cambios bruscos.
