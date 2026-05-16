SHELL := /bin/bash
GRADLE ?= gradle
COMPOSE := docker compose -f infra/docker-compose.local.yml

.PHONY: help infra-up infra-down infra-logs backend-run test build clean health version mongo-shell mongo-migrate

help:
	@echo "Hermes Phase 02 commands"
	@echo "  make infra-up       Start MongoDB replica set, Redis and MinIO"
	@echo "  make infra-down     Stop local infra"
	@echo "  make infra-logs     Follow infra logs"
	@echo "  make backend-run    Run Ktor backend"
	@echo "  make test           Run backend tests"
	@echo "  make build          Build backend"
	@echo "  make health         Call /health"
	@echo "  make version        Call /version"
	@echo "  make mongo-shell    Open mongosh"
	@echo "  make mongo-migrate  Run MongoDB bootstrap migration"

infra-up:
	$(COMPOSE) up -d
	@echo "Waiting a few seconds for MongoDB replica set and MinIO bucket..."
	@sleep 8
	$(COMPOSE) ps

infra-down:
	$(COMPOSE) down -v

infra-logs:
	$(COMPOSE) logs -f

backend-run:
	cd backend && APP_ENV=local APP_VERSION=0.1.0 BUILD_TIME=local COMMIT_SHA=local $(GRADLE) run

test:
	cd backend && $(GRADLE) test

build:
	cd backend && $(GRADLE) clean build

clean:
	cd backend && $(GRADLE) clean

health:
	curl -s http://localhost:8080/health | python3 -m json.tool

version:
	curl -s http://localhost:8080/version | python3 -m json.tool

mongo-shell:
	$(COMPOSE) exec mongo mongosh "mongodb://localhost:27017/hermes_local?replicaSet=rs0"

mongo-migrate:
	$(COMPOSE) exec -T mongo mongosh "mongodb://localhost:27017/hermes_local?replicaSet=rs0" < database/migrations/M001_bootstrap.js
