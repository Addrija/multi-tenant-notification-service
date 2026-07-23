# CLAUDE.md

Development log for this project. See `README.md` for the
user-facing project description; this file is about *how and why* it was
built.

## Architecture decisions

- **No Kafka/Redis/Docker.** An earlier design pass considered these, but
  they directly conflict with the assignment's explicit "no distributed
  systems/microservices, no containerization" constraints, and don't fit
  the current scale and scope of this build. Everything runs in a single Spring Boot process:
  `ThreadPoolTaskExecutor` for the bounded worker pool, an in-process
  round-robin dispatcher for per-tenant fairness, in-memory per-tenant
  token buckets for rate limiting, `@Scheduled` polling + DB columns for
  scheduling/retry, optimistic locking (`@Version`) for dedup on retry.
- **Channels are simulated.** `ChannelSender` interface with one
  implementation per channel (email/SMS/push/in-app), each randomly
  succeeding/failing based on a per-tenant-per-channel configurable rate.
  No real provider integration (Twilio, SES, etc.) — out of scope for the
  assignment.

## Tech stack & reasoning

- **Spring Boot 4.1.0.** Not a deliberate choice for novelty — Spring
  Initializr (`start.spring.io`) rejected every 3.x version requested
  (`bootVersion=3.3.5`, `3.5.6`) with `"Spring Boot compatibility range is
  >=4.0.0"`, i.e. 3.x is no longer served at all as of the build date. Went
  with the current default (4.1.0) rather than fighting the toolchain to
  pin an EOL version, and mitigated the unfamiliarity risk by compiling and
  boot-testing the scaffold immediately rather than assuming Boot 3.x APIs
  would carry over unchanged.
- **MySQL over H2.** Considered switching to embedded H2 (file-based) to
  remove setup friction for the evaluator (no service to install, no
  credentials to configure). Decided against it: the assignment explicitly
  asks for "persistence to a database of your choice," and MySQL is a more
  convincing real answer to that — it's also the author's actual production
  stack. Trade-off accepted: evaluator needs local MySQL running with a
  schema created; documented in README.
- **Credentials never committed.** Repo is public. `application.yml` reads
  `${DB_USERNAME:root}` / `${DB_PASSWORD:}`; actual local credentials live
  in a gitignored `application-local.yml` (activate via
  `SPRING_PROFILES_ACTIVE=local`).

## Assumptions

- `HANDOFF_CONTEXT.md` (present in the working directory, gitignored) is
  private planning/personal-background material from an earlier planning
  session — deliberately excluded from the repo, not a submission artifact.

## RBAC

- Spring Security, HTTP Basic, stateless sessions, BCrypt password hashing.
  `AppUser` backs a custom `UserDetailsService`. Role checks
  (`PLATFORM_ADMIN` vs `TENANT_ADMIN`) are enforced via URL-pattern rules;
  tenant-ownership scoping (a `TENANT_ADMIN` must only touch their own
  tenant) isn't expressible as a URL pattern, so it's enforced in the
  service layer via a `TenantAccessGuard` component instead.
- Found and fixed a schema bug while dumping the SQL for reference:
  `Template.contentTemplate` (a `@Lob String`) had been mapped to MySQL
  `tinytext` (255-byte cap) instead of `longtext`, because combining `@Lob`
  with `@Column(nullable = false)` let `@Column`'s default `length = 255`
  override the LOB sizing. Fixed with an explicit
  `columnDefinition = "LONGTEXT"`.
- Added `sql/01_schema.sql` (schema-only `mysqldump`) and
  `sql/02_seed_data.sql` (demo tenants/users/channels/templates) so an
  evaluator can inspect or recreate the schema and try the API without
  building requests from scratch. Seed passwords are documented in the
  script's header comment.

## REST APIs

- Platform admin (`/api/admin/tenants`) and tenant admin
  (`/api/tenants/{tenantId}/...`) endpoints for tenants, templates, channel
  configs, and notifications, backed by a `GlobalExceptionHandler` mapping
  domain exceptions to consistent HTTP responses (404/403/400/500).
- Discovered Boot 4.1.0 ships **Jackson 3.x**, not the Jackson 2.x package
  layout usually assumed - `ObjectMapper` lives under `tools.jackson.databind`,
  not `com.fasterxml.jackson.databind`. Verified via the actual jar contents
  and Spring's autoconfiguration bytecode before writing code that depended
  on it, rather than assuming API compatibility with Jackson 2.
- Verified the full flow manually against a live instance with seed data:
  tenant/template/channel CRUD, notification submission with template
  variable substitution, delivery-report filtering, and every error path
  (validation, not-found, channel/template mismatch, duplicate channel,
  cross-tenant access denial) - not just unit tests in isolation.

## Dispatch engine

- Built incrementally as small, individually unit-tested commits rather than
  one large commit: rate limiter, retry policy, channel senders, fairness
  scheduler, then the poller/dispatcher - each committed and pushed once its
  own tests passed.
- `NotificationPoller` (scheduled batch fetch + fairness interleave + submit
  to a bounded `ThreadPoolTaskExecutor`) and `NotificationDispatcher` (claim
  + send + outcome recording for one notification) were originally one
  class; split after review into two single-purpose ones.
- Claim step uses `saveAndFlush` (not `save`) specifically so the
  `@Version` optimistic-lock check fires immediately and
  `ObjectOptimisticLockingFailureException` can be caught inline, letting a
  losing concurrent claim skip gracefully instead of failing the whole
  transaction.
- Known gap: notification submission doesn't validate that the tenant has
  a `ChannelConfig` for the requested channel; the dispatcher handles a
  missing/disabled channel defensively (fails the notification permanently
  with a clear reason) but this should ideally be validated earlier, at
  submission time. Left as a documented gap rather than fixed, given the
  current scale and scope of this build.
- Checkpoint verified live (not just unit tests): booted the app, forced
  deterministic outcomes via direct SQL failure-rate overrides, and
  confirmed the full loop end-to-end through the real REST API - immediate
  success, retry/backoff timing (~10s then ~20s, matching the configured
  exponential backoff), and scheduled sends correctly waiting until due.

## User provisioning API

- Gap noticed while planning integration tests: there was no API to create
  `AppUser` accounts, only the SQL seed script. Since the assignment scopes
  "basic RBAC for the two roles" as in-scope and only excludes *advanced*
  auth (OAuth/SSO/MFA), a plain username+password creation endpoint fits
  within scope rather than being an unnecessary addition.
- `POST /api/admin/users` (platform-admin only, covered automatically by
  the existing `/api/admin/**` security rule). Cross-field validation
  (`tenantId` required + must resolve to a real tenant for `TENANT_ADMIN`,
  must be absent for `PLATFORM_ADMIN`) lives in `AppUserService` since it
  can't be expressed as a single-field bean validation annotation.
- Verified live: created a tenant admin through the real endpoint, then
  logged in *as that user* and confirmed both successful access to their
  own tenant and correct 403 on another tenant - the full
  create-then-authenticate-then-authorize loop, not just the creation
  response.

## Open items / not yet decided

(updated as the build progresses)