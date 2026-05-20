# Fase 13B — Smoke test manual de Admin Access API

> Reemplaza `$TOKEN`, `$ORG_ID`, `$USER_ID`, `$ROLE_ID` e `$INVITATION_ID`.

## Headers comunes

```bash
-H "Authorization: Bearer $TOKEN" \
-H "X-Organization-Id: $ORG_ID" \
-H "Content-Type: application/json"
```

---

## 1. Users

### Listar usuarios

```bash
curl -X GET "$BASE_URL/api/v1/admin/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID"
```

### Ver detalle

```bash
curl -X GET "$BASE_URL/api/v1/admin/users/$USER_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID"
```

### Crear usuario temporal

```bash
curl -X POST "$BASE_URL/api/v1/admin/users/temporary" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "cajero.demo@hermes.local",
    "displayName": "Cajero Demo",
    "roleIds": ["role_cashier"],
    "reason": "Alta operativa de caja"
  }'
```

### Actualizar usuario

```bash
curl -X PUT "$BASE_URL/api/v1/admin/users/$USER_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "displayName": "Usuario Actualizado",
    "roleIds": ["role_cashier"],
    "reason": "Actualizar perfil y rol"
  }'
```

### Bloquear

```bash
curl -X POST "$BASE_URL/api/v1/admin/users/$USER_ID/block" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID" \
  -H "Content-Type: application/json" \
  -d '{ "reason": "Salida temporal del negocio" }'
```

### Desbloquear

```bash
curl -X POST "$BASE_URL/api/v1/admin/users/$USER_ID/unblock" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID" \
  -H "Content-Type: application/json" \
  -d '{ "reason": "Reingreso autorizado" }'
```

### Reset password admin

```bash
curl -X POST "$BASE_URL/api/v1/admin/users/$USER_ID/reset-password" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "revokeSessions": true,
    "reason": "Usuario olvidó contraseña"
  }'
```

### Revocar sesiones

```bash
curl -X POST "$BASE_URL/api/v1/admin/users/$USER_ID/revoke-sessions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID" \
  -H "Content-Type: application/json" \
  -d '{ "reason": "Rotación preventiva de acceso" }'
```

---

## 2. Invitations

### Crear invitación

```bash
curl -X POST "$BASE_URL/api/v1/admin/invitations" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nuevo.demo@hermes.local",
    "displayName": "Nuevo Demo",
    "roleIds": ["role_cashier"],
    "reason": "Invitar usuario operativo"
  }'
```

### Listar

```bash
curl -X GET "$BASE_URL/api/v1/admin/invitations" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID"
```

### Detalle

```bash
curl -X GET "$BASE_URL/api/v1/admin/invitations/$INVITATION_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID"
```

### Reenviar

```bash
curl -X POST "$BASE_URL/api/v1/admin/invitations/$INVITATION_ID/resend" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID" \
  -H "Content-Type: application/json" \
  -d '{ "reason": "Usuario solicita reenvío" }'
```

### Revocar

```bash
curl -X POST "$BASE_URL/api/v1/admin/invitations/$INVITATION_ID/revoke" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID" \
  -H "Content-Type: application/json" \
  -d '{ "reason": "Invitación anulada por admin" }'
```

---

## 3. Roles y permisos

### Listar roles

```bash
curl -X GET "$BASE_URL/api/v1/admin/roles" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID"
```

### Crear rol

```bash
curl -X POST "$BASE_URL/api/v1/admin/roles" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "cashier_custom",
    "name": "Cajero personalizado",
    "description": "Puede ver ventas y cobrar",
    "permissionKeys": ["sales.view", "payments.collect"],
    "reason": "Crear rol de caja"
  }'
```

### Actualizar rol

```bash
curl -X PUT "$BASE_URL/api/v1/admin/roles/$ROLE_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Rol actualizado",
    "permissionKeys": ["sales.view", "payments.collect"],
    "reason": "Ajuste de permisos"
  }'
```

### Desactivar rol

```bash
curl -X POST "$BASE_URL/api/v1/admin/roles/$ROLE_ID/deactivate" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID" \
  -H "Content-Type: application/json" \
  -d '{ "reason": "Rol fuera de uso" }'
```

### Activar rol

```bash
curl -X POST "$BASE_URL/api/v1/admin/roles/$ROLE_ID/activate" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID" \
  -H "Content-Type: application/json" \
  -d '{ "reason": "Rol requerido nuevamente" }'
```

### Listar permisos

```bash
curl -X GET "$BASE_URL/api/v1/admin/permissions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Organization-Id: $ORG_ID"
```

---

## Resultado esperado

- Las lecturas devuelven `200 OK`.
- Las mutaciones válidas devuelven `200 OK` o `201 Created`, según el endpoint.
- Las mutaciones sin `reason` deben fallar.
- Las mutaciones sin permiso deben devolver `403 Forbidden`.
- Los cambios que dejen al negocio sin admin o sin gestor de roles deben fallar.
