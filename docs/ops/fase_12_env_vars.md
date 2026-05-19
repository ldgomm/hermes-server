# Fase 12 — Variables de entorno y configuración operativa

## Objetivo

Documentar las variables/configuraciones que deben existir para operar la Fase 12 en local, staging, pruebas SRI y producción controlada.

Los nombres exactos pueden adaptarse al sistema de configuración del proyecto, pero la intención de seguridad debe mantenerse.

---

## Backend base

| Variable | Ejemplo | Requerida | Nota |
|---|---:|---:|---|
| `HERMES_ENV` | `local`, `staging`, `production` | Sí | Ambiente de despliegue interno |
| `HERMES_BASE_URL` | `http://localhost:8080` | Sí | URL pública/base del backend |
| `JWT_SECRET` | secreto largo | Sí | Nunca commitear |
| `MONGO_URI` | `mongodb://localhost:27017/hermes` | Sí | Usar replica set si se prueban transacciones |
| `MONGO_DATABASE` | `hermes` | Sí | Base del proyecto |

---

## Facturación electrónica/SRI

| Variable | Ejemplo | Requerida | Nota |
|---|---:|---:|---|
| `SRI_PRODUCTION_GLOBALLY_ENABLED` | `false` | Sí | Debe ser `false` por defecto |
| `SRI_DEFAULT_ENVIRONMENT` | `TEST` | Sí | No asumir producción |
| `SRI_TEST_RECEPTION_WSDL_URL` | configurado por deployment | Sí para TEST | No quemar en código productivo |
| `SRI_TEST_AUTHORIZATION_WSDL_URL` | configurado por deployment | Sí para TEST | No quemar en código productivo |
| `SRI_PROD_RECEPTION_WSDL_URL` | configurado por deployment | Sí para PROD | Solo production real controlada |
| `SRI_PROD_AUTHORIZATION_WSDL_URL` | configurado por deployment | Sí para PROD | Solo production real controlada |
| `SRI_SOAP_CONNECT_TIMEOUT_MS` | `10000` | No | Timeout defensivo |
| `SRI_SOAP_READ_TIMEOUT_MS` | `30000` | No | Timeout defensivo |

---

## Firma electrónica

| Variable | Ejemplo | Requerida | Nota |
|---|---:|---:|---|
| `SIGNATURE_SECRET_MASTER_KEY` | secreto KMS/local | Sí | Cifrado de passwords/secretos |
| `SIGNATURE_STORAGE_PROVIDER` | `local`, `s3`, `gcs`, `mongo-gridfs` | Sí | Según implementación |
| `SIGNATURE_LOCAL_STORAGE_PATH` | `/var/hermes/signatures` | Solo local | Nunca exponer por API |
| `SIGNATURE_EXPIRY_WARNING_DAYS` | `30` | No | Para readiness/admin |

Reglas:

- Nunca loguear password de firma.
- Nunca devolver bytes de `.p12`.
- Nunca devolver object key interno.
- Rotar claves si hay incidente.

---

## Artefactos XML/RIDE

| Variable | Ejemplo | Requerida | Nota |
|---|---:|---:|---|
| `ELECTRONIC_ARTIFACT_STORAGE_PROVIDER` | `local`, `s3`, `gcs`, `mongo-gridfs` | Sí | XML/RIDE |
| `ELECTRONIC_ARTIFACT_LOCAL_PATH` | `/var/hermes/electronic-artifacts` | Solo local | No exponer |
| `ELECTRONIC_ARTIFACT_ENCRYPTION_ENABLED` | `true` | Recomendado | Especialmente XML autorizado |
| `ELECTRONIC_ARTIFACT_MAX_SIZE_MB` | `10` | No | Protección |

---

## Email

| Variable | Ejemplo | Requerida | Nota |
|---|---:|---:|---|
| `EMAIL_PROVIDER` | `smtp`, `ses`, `sendgrid`, `noop` | Sí | `noop` para local/tests |
| `EMAIL_FROM` | `facturacion@example.com` | Sí | Remitente técnico |
| `EMAIL_REPLY_TO` | `soporte@example.com` | No | Puede venir de settings |
| `SMTP_HOST` | `smtp.example.com` | Según provider | No commitear |
| `SMTP_PORT` | `587` | Según provider |  |
| `SMTP_USERNAME` | `user` | Según provider | Secreto |
| `SMTP_PASSWORD` | `secret` | Según provider | Secreto |

---

## Homologación

| Variable | Ejemplo | Requerida | Nota |
|---|---:|---:|---|
| `HOMOLOGATION_ENABLED` | `true` | Sí | Puede apagarse en producción |
| `HOMOLOGATION_ALLOW_FAKE_SRI` | `true` | Solo local/test | Nunca para validación real |
| `HOMOLOGATION_REPORT_STORAGE_PATH` | `/var/hermes/homologation` | Según storage | Reportes `.md` |
| `HOMOLOGATION_MAX_RUNS_PER_DAY` | `10` | No | Protección operativa |

---

## Seguridad mínima por ambiente

### Local

- `SRI_PRODUCTION_GLOBALLY_ENABLED=false`
- Email en `noop` o sandbox.
- Firma de prueba.
- Fake/mock SRI permitido para tests.

### Staging

- `SRI_PRODUCTION_GLOBALLY_ENABLED=false`
- Ambiente SRI TEST.
- Email sandbox.
- Firma de prueba o certificado controlado.
- Homologación técnica habilitada.

### Production

- `SRI_PRODUCTION_GLOBALLY_ENABLED=true` solo después de aprobación operativa.
- Producción por organización debe seguir deshabilitada hasta gate.
- Secrets en vault/KMS.
- Logs sin secretos.
- Backups activos.
- Monitoreo de errores SRI/email/storage.

