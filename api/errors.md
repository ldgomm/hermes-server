# Hermes API Errors

## Formato estándar

```json
{
  "error": {
    "code": "domain_rule_violation",
    "message": "Cannot close an unpaid sale.",
    "requestId": "req_...",
    "details": null
  }
}
```

## Códigos base

| HTTP | code | Uso |
|---:|---|---|
| 400 | bad_request | JSON inválido, parámetros inválidos |
| 401 | unauthenticated | No hay token o es inválido |
| 403 | forbidden | Usuario sin permiso |
| 404 | not_found | Recurso inexistente o fuera de tenant |
| 409 | conflict | Versión, duplicado, transición inválida |
| 422 | domain_rule_violation | Regla de negocio rota |
| 500 | internal_error | Error inesperado |
| 503 | service_unavailable | Infraestructura no disponible |

## Reglas

1. `DomainRuleViolation` debe responder 422.
2. Conflictos de versión deben responder 409.
3. Recursos fuera de organización deben responder 404 o 403 según política de seguridad.
4. Nunca exponer stack traces.
5. Siempre devolver `requestId`.
