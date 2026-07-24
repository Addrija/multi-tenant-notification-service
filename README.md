# Multi-Tenant Notification Service

A multi-tenant notification service supporting email, SMS, push, and in-app
channels, tenant-defined templates with variable substitution, scheduled and
immediate sends, per-tenant rate limiting, retries with exponential backoff,
and delivery tracking with a full audit trail. Built with Spring Boot.

See `CLAUDE.md` for the development log (decisions made and why, kept
up to date as the build progressed).

## Tech stack

- **Java 21**, **Spring Boot 4.1.0** (current default at build time — Spring
  Initializr no longer serves 3.x)
- **Spring Data JPA / Hibernate**, **MySQL** for persistence
- **Spring Security** (HTTP Basic) for RBAC
- **Maven**

## Architecture at a glance

Everything runs in a single Spring Boot process — no Kafka, Redis, or
containers, per the assignment's explicit constraints:

| Concern | Implementation |
|---|---|
| Bounded worker pool | `ThreadPoolTaskExecutor` |
| Per-tenant fairness | `NotificationPoller` round-robins a due batch across tenants before submitting to the pool |
| Per-tenant rate limiting | In-memory per-tenant token bucket, lazy time-based refill |
| Scheduled + immediate sends | `scheduledAt` column + `@Scheduled` poller |
| Dedup on retry | Stable notification ID + optimistic locking (`@Version`) — a losing concurrent claim is skipped, not retried |
| Retry with backoff | Exponential backoff (capped), tracked via `attemptCount`/`nextRetryAt` |
| Audit trail | `notification_status_history` (state transitions) + `delivery_attempts` (per-attempt outcomes) — two separate tables since the requirement names both concerns separately |
| Channels | `ChannelSender` interface, one implementation per channel, all **simulated** (see Assumptions) |
| RBAC | Spring Security + HTTP Basic, `PLATFORM_ADMIN` / `TENANT_ADMIN` roles, tenant-scoping enforced in the service layer |

## Setup

1. **MySQL** running locally, with a database created:
   ```sql
   CREATE DATABASE notification_service CHARACTER SET utf8mb4;
   ```
2. **Credentials**: create `src/main/resources/application-local.yml` (gitignored) with:
   ```yaml
   spring:
     datasource:
       username: root
       password: "your-mysql-password"
   ```
   (Alternatively, export `DB_USERNAME` / `DB_PASSWORD` env vars — `application.yml` reads from either.)
3. **Load schema + demo data** (schema is also auto-created by Hibernate on boot, but the scripts are provided for reference/manual setup):
   ```bash
   mysql -uroot -p notification_service < sql/01_schema.sql
   mysql -uroot -p notification_service < sql/02_seed_data.sql
   ```
4. **Build**:
   ```bash
   ./mvnw clean package
   ```
   Produces `target/multi-tenant-notification-service-0.0.1-SNAPSHOT.jar`. Add `-DskipTests` if you just want the jar without running the test suite (see Testing below for running tests on their own).
5. **Run** — either via Maven directly, or the built jar:
   ```bash
   SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
   # or
   SPRING_PROFILES_ACTIVE=local java -jar target/multi-tenant-notification-service-0.0.1-SNAPSHOT.jar
   ```
   App comes up on `http://localhost:8081` (8080 was occupied by an unrelated process during development, so the default was moved — override with `SERVER_PORT` if needed).

## Demo credentials (from `sql/02_seed_data.sql`)

| Username | Password | Role | Tenant |
|---|---|---|---|
| `platformadmin` | `PlatformAdmin@123` | `PLATFORM_ADMIN` | — |
| `acme_admin` | `TenantAdmin@123` | `TENANT_ADMIN` | Acme Corp |
| `globex_admin` | `TenantAdmin@123` | `TENANT_ADMIN` | Globex Inc |

All endpoints use HTTP Basic auth, e.g.:
```bash
curl -u acme_admin:TenantAdmin@123 http://localhost:8081/api/tenants/2/templates
```

## Postman collection

`postman/multi-tenant-notification-service.postman_collection.json` covers
every endpoint, organized into folders in a natural demo order (platform
admin → user provisioning → templates → channels → notifications →
security/RBAC). Collection variables default to the seeded Acme Corp
tenant/credentials, and IDs from creation requests (tenant, template,
channel config, notification) auto-save into variables so most of the flow
runs top-to-bottom without manual copy-pasting. Each request's description
explains what to send and what response to expect. Import it into Postman,
have the app running locally with seed data loaded, and go.

## API overview

**Platform admin** (`PLATFORM_ADMIN` only):
- `POST/GET /api/admin/tenants`, `GET /api/admin/tenants/{id}`
- `PUT /api/admin/tenants/{id}/rate-limits`
- `POST /api/admin/users` — create platform admins or tenant admins

**Tenant admin** (scoped to their own tenant — a `TENANT_ADMIN` gets a 403 on any other tenant's data):
- `POST/GET/PUT/DELETE /api/tenants/{tenantId}/templates`
- `POST/GET/PUT /api/tenants/{tenantId}/channels` (no delete — disable via `PUT {enabled:false}`)
- `POST /api/tenants/{tenantId}/notifications` — submit (immediate or scheduled via `scheduledAt`)
- `GET /api/tenants/{tenantId}/notifications` — delivery report, filterable by `status`/`channelType`
- `GET /api/tenants/{tenantId}/notifications/{id}/history` — full audit trail

## Testing

```bash
./mvnw test
```

61 tests total:
- **Unit tests** (mocked dependencies) for the rate limiter, retry/backoff policy, channel senders, fairness scheduler, dispatcher, template renderer, and services.
- **Integration tests** (`src/test/java/.../integration/`) boot the full Spring context against the same local MySQL instance — real Spring Security filter chain, real `@Scheduled` poller, no mocks. They create and clean up their own isolated tenant/user fixtures directly via repositories (not dependent on the seed script), so they're safe to re-run repeatedly. The flagship test (`NotificationLifecycleIntegrationTest`) submits a real notification via the REST API and uses Awaitility to wait for the actual background poller to dispatch it — forcing deterministic outcomes via `simulatedFailureRate` overrides (0.0/1.0) rather than relying on random luck.

Integration tests require the same local MySQL + `local` profile setup as running the app (see Setup above).

## Assumptions

- **Channels are simulated.** No real email/SMS/push provider integration (Twilio, SES, etc.) — out of scope per the assignment. Each channel randomly succeeds/fails based on a per-tenant-per-channel `simulatedFailureRate` (configurable via the channel config API), which makes the retry/backoff/audit-trail behavior demonstrable on demand.
- **Notifications are template-based.** Every notification submission requires a `templateId`; there's no "raw content" send path. This keeps the templating feature (a named requirement) central rather than optional.
- **Channel existence isn't validated at submission time.** If a tenant submits a notification for a channel they haven't configured (or has been disabled), the dispatcher catches this defensively when it picks the notification up (marks it permanently `FAILED` with a clear reason) rather than rejecting it at the `POST` — a gap worth closing in a follow-up but left as documented behavior here.
- **User provisioning is basic, not self-service.** `POST /api/admin/users` (platform-admin only) is the only way to create accounts — no signup flow, no OAuth/SSO/MFA (explicitly out of scope).
- **Rate limits are per-tenant, not per-tenant-per-channel.** A tenant with no `RateLimitConfig` set is treated as unlimited rather than blocked, since a platform admin not having configured a limit yet shouldn't silently stop all sends.
- **MySQL over an embedded database.** The assignment asks for "persistence to a database of your choice" — MySQL was chosen as a more convincing real answer to that than an embedded option, at the cost of requiring the evaluator to have MySQL running locally (see Setup).

## AI workflow

Built with Claude Code. See `CLAUDE.md` for the running log of decisions
made during development — tech stack choices and why, bugs found and fixed
along the way (e.g. a schema column-sizing bug, a Jackson 3 package-location
surprise in Boot 4.1.0), and the incremental branch-per-feature /
commit-per-piece workflow used throughout. No specialized Claude Code Skills
were applicable to this backend Java build; work was done directly via the
CLI's file/shell tools with continuous compile-and-test verification at
each step, plus live manual verification against a running instance at key
checkpoints (not just unit tests in isolation).
