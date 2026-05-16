# Hermes Business Platform — Phase 02 Backend Bootstrap

Base técnica inicial para el backend de la plataforma móvil para pequeños negocios en Ecuador.

Esta entrega corresponde a **Fase 2 — Bootstrap técnico backend** y deja listo un backend Ktor mínimo con:

- Kotlin + Ktor.
- Gradle Kotlin DSL.
- MongoDB local con replica set.
- Redis local.
- MinIO local.
- Configuración por ambientes.
- Logging estructurado básico.
- Endpoints iniciales `/health` y `/version`.
- OpenAPI mínimo.
- Testcontainers configurado.
- GitHub Actions básico.
- Dockerfile backend.
- Makefile operativo.

## Requisitos locales

- JDK 21.
- Docker Desktop o Docker Engine.
- Gradle 8.x instalado localmente.

> Si prefieres usar Gradle Wrapper, ejecuta una sola vez dentro de `backend/`:
>
> ```bash
> gradle wrapper --gradle-version 8.10.2
> ```
>
> Luego puedes cambiar `GRADLE=gradle` por `GRADLE=./gradlew` en el Makefile o ejecutar `make GRADLE=./gradlew backend-run`.

## Arranque rápido

Desde la raíz del repo:

```bash
cp .env.example .env
make infra-up
make backend-run
```

En otra terminal:

```bash
curl http://localhost:8080/health
curl http://localhost:8080/version
```

## Comandos principales

```bash
make infra-up        # levanta MongoDB replica set, Redis y MinIO
make infra-down      # apaga la infraestructura local
make infra-logs      # muestra logs de infraestructura
make backend-run     # corre Ktor localmente
make test            # ejecuta tests backend
make build           # compila backend
make health          # prueba /health
make version         # prueba /version
make mongo-shell     # abre mongosh
make mongo-migrate   # ejecuta migración bootstrap MongoDB
```

## Variables principales

El backend lee variables de ambiente. Para local se usan defaults seguros de desarrollo:

```bash
APP_ENV=local
APP_VERSION=0.1.0
PORT=8080
MONGODB_URI=mongodb://localhost:27017/hermes_local?replicaSet=rs0
MONGODB_DATABASE=hermes_local
REDIS_URI=redis://localhost:6379/0
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=hermes_minio
MINIO_SECRET_KEY=hermes_minio_secret
MINIO_HEALTH_BUCKET=hermes-health
```

## Endpoints

### `GET /health`

Devuelve estado general y checks de dependencias.

```json
{
  "status": "UP",
  "timestamp": "2026-05-15T00:00:00Z",
  "checks": [
    { "name": "application", "status": "UP", "message": "Application is running" },
    { "name": "mongodb", "status": "UP", "message": "MongoDB ping OK" },
    { "name": "redis", "status": "UP", "message": "Redis ping OK" },
    { "name": "minio", "status": "UP", "message": "MinIO bucket exists" }
  ]
}
```

### `GET /version`

Devuelve metadata del build.

```json
{
  "appName": "Hermes Business Platform API",
  "version": "0.1.0",
  "environment": "local",
  "buildTime": "local",
  "commitSha": "local"
}
```

## Criterio de salida de Fase 2

La fase queda lista cuando esto funcione:

```bash
make infra-up
make mongo-migrate
make backend-run
make health
make version
make test
make build
```

Y se cumpla:

- `/health` responde.
- `/version` responde.
- MongoDB replica set está activo.
- Redis responde `PONG`.
- MinIO tiene bucket `hermes-health`.
- Tests pasan.
- GitHub Actions compila.

## Qué NO incluye esta fase

Esta base no implementa todavía:

- Auth/JWT.
- Empresas/organizaciones.
- Roles/permisos.
- Ventas.
- Caja.
- Inventario.
- Tax Engine.
- Firma electrónica.
- SRI.
- XML/RIDE.

Eso empieza desde Fase 3 y Fase 4.
