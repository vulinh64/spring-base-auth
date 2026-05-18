# spring-base-auth

## Description

JWT issuer and authentication server for the `spring-base` ecosystem. It owns user identity and credential management, mints access/refresh tokens, and exposes the standard endpoints resource servers need to validate them:

- **OIDC discovery** at `/.well-known/openid-configuration`

- **JWKS** at `/.well-known/jwks.json`

- **Authentication endpoints** under `/auth/**` (login, refresh, logout)

- **Account management endpoints**

It also provisions the shared PostgreSQL schemas for downstream services (e.g. `spring-base`, `spring-base-event`) on first boot, so peer services can start against an already-prepared database.

## Requirements for local debugging

| Component       | Notes                                                                                                                |
|-----------------|----------------------------------------------------------------------------------------------------------------------|
| JDK 25          | Required to build and run.                                                                                           |
| Maven           | The wrapper (`mvnw` / `mvnw.cmd`) is included — no global install needed.                                            |
| PostgreSQL      | Application database. This service also bootstraps peer-service databases on the same instance.                      |
| RabbitMQ        | Message broker (key-rotation events, outbound notifications).                                                        |
| Commons library | [vulinh64/spring-base-commons](https://github.com/vulinh64/spring-base-commons) — shared data classes and utilities. |

## Setup

### Step 1 — Install the commons library

The commons artifact is published as a GitHub release and must be installed into your local Maven repository before the project can resolve its dependencies.

- **Windows:** run [`create-data-classes.cmd`](./create-data-classes.cmd)

- **Linux / macOS:** run [`create-data-classes.sh`](./create-data-classes.sh)

### Step 2 — Initialize PostgreSQL and RabbitMQ containers

This service is the canonical owner of the shared database — it provisions schemas for itself and peer services (`spring-base`, `spring-base-event`) on first boot. Running this step here is what makes the rest of the ecosystem bootable.

- **Windows:** run [`initialize-postgres-rabbitmq.cmd`](./initialize-postgres-rabbitmq.cmd)

- **Linux / macOS:** run [`initialize-postgres-rabbitmq.sh`](./initialize-postgres-rabbitmq.sh)

## Configuration

Sensible defaults are already wired up in [`application.yaml`](./src/main/resources/application.yaml), so the app boots out of the box against the containers from Step 2. Override any of the following environment variables when the defaults do not match your setup:

| Environment variable          | Default                            | Remark                                                                                              |
|-------------------------------|------------------------------------|-----------------------------------------------------------------------------------------------------|
| `ISSUER_SERVER`               | `http://localhost:8080`            | Public base URL of this auth server. Baked into the JWT `iss` claim and the OIDC discovery doc.     |
| `DISCOVERY_PATH`              | `/.well-known/openid-configuration`| Path of the OIDC discovery document.                                                                |
| `JWKS_PATH`                   | `/.well-known/jwks.json`           | Path of the JWKS endpoint.                                                                          |
| `TOKEN_DELIVERY`              | `COOKIE`                           | How tokens are returned to clients — `COOKIE` for HttpOnly cookies, `BODY` for JSON.                |
| `COOKIE_SECURE`               | `true`                             | Sets the `Secure` flag on auth cookies. Leave `true` outside of plain-HTTP local debugging.         |
| `ACCESS_TOKEN_COOKIE_NAME`    | `access_token`                     | Cookie name carrying the access token.                                                              |
| `REFRESH_TOKEN_COOKIE_NAME`   | `refresh_token`                    | Cookie name carrying the refresh token.                                                             |
| `BOOTSTRAP_PEER_DATABABSE`    | `true`                             | Provision peer-service schemas on startup. Disable once the shared database is already initialized. |
| `SPRING_BASE_USER`            | _datasource user_                  | Owner role for the `spring-base` schema during peer-database bootstrap.                             |
| `SPRING_BASE_EVENT_USER`      | _datasource user_                  | Owner role for the `spring-base-event` schema during peer-database bootstrap.                       |
| `LIQUIBASE_ENABLED`           | `true`                             | Run Liquibase migrations on startup.                                                                |
| `POSTGRES_HOST`               | `localhost`                        | PostgreSQL host.                                                                                    |
| `POSTGRES_PORT`               | `5432`                             | PostgreSQL port.                                                                                    |
| `POSTGRES_USERNAME`           | `postgres`                         | Database user.                                                                                      |
| `POSTGRES_PASSWORD`           | `123456`                           | Database password.                                                                                  |
| `RABBITMQ_HOST`               | `localhost`                        | RabbitMQ host.                                                                                      |
| `RABBITMQ_PORT`               | `5672`                             | RabbitMQ AMQP port.                                                                                 |
| `RABBITMQ_USERNAME`           | `rabbitmq`                         | RabbitMQ user.                                                                                      |
| `RABBITMQ_PASSWORD`           | `123456`                           | RabbitMQ password.                                                                                  |

> [!WARNING]
> `ISSUER_SERVER` must be set to the URL at which **other services reach this auth server**, not the URL you use from your host machine. On a shared docker-compose network that typically looks like `http://spring-base-auth:8080` (the compose service name), not `http://localhost:8080`.
>
> The issuer value is baked into every minted JWT's `iss` claim and into the discovery document's `issuer` / `jwks_uri` fields. If it doesn't match what resource servers see, JWT validation will fail with an issuer mismatch and JWKS fetches will hit the wrong host.
>
> Resource servers consuming these tokens must point `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` (or `issuer-uri`, which auto-discovers JWKS) at the same `ISSUER_SERVER` value — e.g. `spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://spring-base-auth:8080/.well-known/jwks.json`.
