# AI Customer Support Platform

[![CI](https://github.com/shaeniR/AI-Customer-Support-Platform/actions/workflows/ci.yml/badge.svg)](https://github.com/shaeniR/AI-Customer-Support-Platform/actions/workflows/ci.yml)

A production-style customer support platform for **LankaMart**, a fictional Sri Lankan online store.
Customers chat with an AI assistant that answers from company policies and real order data, and
serious issues are escalated to human agents as tickets.

> LankaMart is fictional. All data is test data.

## Tech stack

| Layer      | Technology                                        |
|------------|---------------------------------------------------|
| Backend    | Java 21, Spring Boot 4, Maven                     |
| Frontend   | Next.js (App Router, TypeScript) + Tailwind CSS   |
| Database   | MySQL 8.4, Flyway migrations                      |
| Cache      | Redis                                             |
| API docs   | springdoc-openapi (Swagger UI)                    |
| CI         | GitHub Actions                                    |

## Repository layout

```
backend/     Spring Boot API (package-by-feature: auth, user, conversation, ticket, order, knowledge, ai, ...)
frontend/    Next.js app (customer, agent and admin screens)
docker-compose.yml   Local MySQL + Redis
.env.example         All environment variables (names and safe local defaults)
```

## Run locally

Prerequisites: Java 21, Node.js 20+, Docker.

```bash
# 1. Start MySQL and Redis
cp .env.example .env
docker compose up -d

# 2. Start the backend (Flyway runs migrations on startup)  -> http://localhost:8080
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run

# 3. Start the frontend  -> http://localhost:3000
cd frontend
npm install
npm run dev
```

Useful URLs:

- Health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html

## Configuration

All backend settings are in `backend/src/main/resources/application.yml`. Every value can be
overridden with an environment variable (for example `DB_URL`, `REDIS_URL`, `JWT_SECRET`).
See `.env.example` for the full list. Never commit real secrets.

## Tests

```bash
cd backend && ./mvnw verify      # integration tests use Testcontainers (need Docker; skipped without it)
cd frontend && npm run lint && npm run build
```

CI runs both on every push.
