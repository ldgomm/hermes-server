# Fase 13B.6 — Roles & Permissions Admin API

## Objetivo

Cerrar el bloque de administración de roles y permisos de la Fase 13B con:

- activación y desactivación de roles personalizados;
- actualización segura de nombre, descripción y permisos;
- bloqueo de cambios que dejen al negocio sin administrador;
- bloqueo de cambios que dejen al negocio sin gestor de roles;
- pruebas de rutas para roles y catálogo de permisos;
- contrato claro para Admin iOS/Web Admin.

---

## Endpoints cerrados

```http
GET  /api/v1/admin/roles
POST /api/v1/admin/roles
GET  /api/v1/admin/roles/{roleId}
PUT  /api/v1/admin/roles/{roleId}
POST /api/v1/admin/roles/{roleId}/activate
POST /api/v1/admin/roles/{roleId}/deactivate

GET  /api/v1/admin/permissions
```

---

## Decisión de seguridad

Un rol de organización personalizado puede editarse solo si:

1. pertenece a la organización activa;
2. es `CUSTOM`;
3. no es `systemRole`;
4. no es `critical`;
5. es `editable`;
6. no está `ARCHIVED`.

Además, el sistema debe impedir dos accidentes operativos:

### 1. Dejar a la organización sin administrador efectivo

No se permite actualizar o desactivar el último rol activo que otorga permisos administrativos.

Permisos considerados administrativos:

```text
*
credentials.users.create
credentials.users.invite
credentials.roles.manage
organization.update
```

### 2. Dejar a la organización sin gestor de roles

Aunque todavía exista un admin con otros permisos, no se permite quitar el último permiso capaz de administrar roles.

Permisos considerados de gestión de roles:

```text
*
credentials.roles.manage
```

Esto evita que el negocio conserve un “admin” operativo pero pierda la capacidad de corregir roles/permisos desde la API.

---

## Permisos por endpoint

| Endpoint | Permiso requerido |
|---|---|
| `GET /api/v1/admin/roles` | `credentials.roles.view` |
| `GET /api/v1/admin/roles/{roleId}` | `credentials.roles.view` |
| `POST /api/v1/admin/roles` | `credentials.roles.manage` |
| `PUT /api/v1/admin/roles/{roleId}` | `credentials.roles.manage` |
| `POST /api/v1/admin/roles/{roleId}/activate` | `credentials.roles.manage` |
| `POST /api/v1/admin/roles/{roleId}/deactivate` | `credentials.roles.manage` |
| `GET /api/v1/admin/permissions` | `credentials.roles.view` |

---

## Contrato de update seguro

Ejemplo:

```json
{
  "name": "Caja principal",
  "description": "Puede ver ventas y cobrar",
  "permissionKeys": [
    "sales.view",
    "payments.collect"
  ],
  "reason": "Ajuste de permisos operativos"
}
```

Reglas:

- `reason` es obligatorio.
- `permissionKeys` no puede estar vacío si se envía.
- no se aceptan permisos desconocidos;
- no se acepta wildcard `*` en roles custom de organización;
- no se editan roles platform/system/critical;
- no se permite dejar el negocio sin admin ni sin gestor de roles.

---

## Respuesta de permisos

`GET /api/v1/admin/permissions` devuelve DTOs públicos y no entidades internas:

```json
{
  "permissions": [
    {
      "code": "credentials.users.view",
      "name": "View users",
      "description": "...",
      "category": "CREDENTIALS",
      "scope": "ORGANIZATION",
      "riskLevel": "LOW",
      "status": "ACTIVE",
      "systemManaged": true,
      "requiresAudit": false,
      "requiresReason": false,
      "requiresStepUp": false,
      "featureFlag": null
    }
  ]
}
```

Por defecto se devuelven permisos activos. Los reservados se consultan solo con:

```http
GET /api/v1/admin/permissions?includeReserved=true
```

---

## Criterio de salida de 13B.6

- Use cases de roles protegidos contra pérdida del último admin.
- Use cases protegidos contra pérdida del último gestor de roles.
- Rutas `PUT`, `activate`, `deactivate` cubiertas.
- Ruta de permisos cubierta.
- Contrato documentado para Admin iOS/Web Admin.
- `./gradlew clean test` verde.
