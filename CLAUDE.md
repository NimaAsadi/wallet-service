# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A wallet/ledger microservice for a trading platform (group `ir.ebb`, root project `wallet-service`). **Java 21 + Apache Pekko**, multi-module Gradle build. The wallet is an **event-sourced, cluster-sharded aggregate** (`WalletActor`); the **event journal is the source of truth**; single-wallet reads (`getWallet`/`getBuyingPower`) are served **strongly-consistent from the entity**, while bulk/list reads use the read-model tables (currently **frozen** — the CQRS projections were removed pending a new projection for the next-gen actor); the cluster runs on Kubernetes via Cluster Bootstrap. **No Spring, no JPA.** See `ARCHITECTURE.md` for the full design.

Module dependency graph:
```
base ◀── core
core ◀── external
{core, external, base} ◀── wallet-app   (wallet-app is the only app/entry point)
```

Dependencies resolve from Maven Central (+ the Gradle Plugin Portal for plugins). The private Nexus URL `nexus.ebidar.net` is present but **commented out** in `settings.gradle` and the root `build.gradle`, so it is not active. Two **internal `ir.ebb` libraries** (`ir.ebb:common`, `ir.ebb.oms:slerlc`) were dropped during the Spring removal and their types **re-homed into the `base` module under the same packages** (`ir.ebb.common.*`, `ir.ebb.oms.*`) as plain POJOs — so 60+ files needed no import changes.

## Build / Run / Test

All commands via the wrapper (`./gradlew`). Java 21 toolchain is enforced via `JavaLanguageVersion.of(21)` in the root `build.gradle`; there is no foojay/auto-provisioning plugin, so a local JDK 21 must be available on the Gradle toolchain.

```bash
./gradlew build                      # compile + test all modules
./gradlew compileJava                # fast typecheck across modules
./gradlew :wallet-app:installDist    # build the runnable distribution
./gradlew :core:test                 # all core tests (WalletAggregateTest + WalletActorTest)
./gradlew :core:test --tests "ir.ebb.wallet.wallet.WalletActorTest"          # one class
./gradlew :core:test --tests "ir.ebb.wallet.wallet.WalletActorTest#restart*" # one method/glob
```

Run: `build/install/wallet-app/bin/wallet-app` (needs Postgres + Keycloak + Kafka reachable; forms a Pekko cluster). Add `--seed-from-legacy` once to import existing wallet rows into the journal (idempotent).

The `testBundle` (JUnit 5 + AssertJ + Testcontainers Postgres) is wired into every module. `pekko-persistence-testkit` is on `core` (used by `WalletActorTest`).

Run-time config is externalized via env vars / HOCON `application.conf`. Defaults: HTTP `:8080`, gRPC `:9090`, management `:8558`, artery `:25520`. The journal/snapshot/projections use **`pekko-persistence-r2dbc`** over the same Postgres (reactive R2DBC pool, tables in the `pekko` schema); the read-side repos use a separate **HikariCP pool of 80** (blocking JDBC). Hibernate is gone (schema owned by Liquibase — **never rely on JPA to create tables**).

## Module Architecture

```
base        ← foundational infra: ir.ebb.base.jdbc.Jdbc / exceptions / security + re-homed ir.ebb.common.* POJOs/enums
  ▲
core        ← wallet domain: event-sourced entity + protocol + read-side repos/query services
  ▲
external    ← Rayan HTTP gateway integration (Pekko HTTP client + COPY sync)
  ▲
wallet-app  ← the ONLY app; Dagger component (`app/di`) + lifecycle root (Main), HTTP/gRPC/cron, infrastructure wiring
```

`wallet-app` depends on `core`, `external`, `base`. `.proto` files live in `wallet-app/src/main/proto`; generated sources are added to the `main` sourceSet.

## Core Concepts (read multiple files before changing these)

### 1. Event-sourced WalletActor (`core/.../wallet/`)
Every wallet is a sharded `EventSourcedBehavior<WalletCommand, WalletEvent, WalletState>` — **exactly one per `accountNumber` across the cluster**, located **only** via Cluster Sharding (no actor registry).

- **Command handler**: validate → `state.toAggregate()` (rebuild the mutable `aggregate/Wallet`) → run the existing money-movement cascade (deposit/freeze/unfreeze/spend/...) → `WalletState.fromAggregate(...)` → `Effect().persist(WalletEvent.WalletMutated)`. Domain exceptions → `WalletReply.Rejected(code)`. **The entity performs no DB I/O** (persistence is handled by the R2DBC journal plugin).
- **Event handler**: `state = event.resultingState()` (absolute values → **idempotent**).
- **Snapshotting**: every 100 events (`retentionCriteria`).
- **Supervision**: backoff restart on failure (recovers from journal).
- **Passivation**: 10-min receive-timeout → internal `WalletCommand.Passivate` → `Effect().stop()`; sharding recreates from the journal on the next message.
- **Idempotency**: duplicate `trackingId` (recently seen, ring of 1024) → `Rejected(4007)`.

The mutable `aggregate/Wallet` cascade (deposit's separ-credit → credit → `settleDebt` waterfall, `freeze`/`applyFreeze` borrowing, `unfreeze`, `spend`, `buyingPower(settlementDelay)`) is **reused verbatim** — it is the single source of money-movement logic for the live `wallet/` path, guarded by `WalletAggregateTest` (whose name predates the new aggregate below — it exercises this `Wallet` cascade, not `WalletAggregate`).

### 1b. Next-gen aggregate `ir.ebb.wallet.actor` (in progress — NOT yet wired into the app)
`core/.../actor/` (`ir.ebb.wallet.actor.WalletActor` + `actor/command/` + `actor/event/`) and its sibling `core/.../aggregate/` (`ir.ebb.wallet.aggregate.WalletAggregate`) are the intended successor to `core/.../wallet/`, actively built on this branch. **It is not wired into `Main`/`ProjectionBootstrap`** — `Main` still boots `wallet.WalletActor` (`EntityTypeKey("wallet")`). Until the cutover both aggregates coexist; treat `wallet/` as live and `actor/` as the migration target.
- **Different ES shape — behavior on the aggregate, one event per operation.** `aggregate/WalletAggregate` *is* the event-sourced `State` and holds all logic: `Try<WalletEvent> validate(WalletCommand)` (command → event; throws `BusinessException` for `INVALID_COMMAND`/`DUPLICATE_TRACKING_ID`/`INSUFFICIENT_BALANCE`/`INSUFFICIENT_FREEZE`) and `WalletAggregate applyEvent(WalletEvent)` (mutates `this`, returns it). `actor/WalletActor` is thin: `handleCommand` = `aggregate.validate(cmd).map(e -> Effect().persist(e).thenReply(ack)).recover(t -> ...error(t))`; the event handler just calls `aggregate.applyEvent`. This is the inverse of the legacy actor, which runs the cascade itself and persists a single `WalletMutated`.
- **Granular delta events** (not absolute state): `WalletCreated`, `Deposited`, `Frozen`, `Unfrozen`, `Spent` — records of `(trackingId, value, settlementDelay, walletTransactionType, canSpendSeparCredit, dbsAccountNumber)`. `BalanceUnfrozen.applyEvent` composes a `Spent` + `Deposited`. Commands mirror them (`CreateWallet(dbsAccountNumber)`, `DepositBalance`, `Freeze`, `Spend`, `Unfreeze`); a stray `Deposit` record exists but is **not wired** into the command handler.
- **Money model preserved (re-implemented, not shared)**: `t0/t1/t2` `WalletParameter` (balance+frozen per settlement delay), `credit`/`separCredit`/`initialCredit`/`separInitialCredit`, `WalletDebt`, `BuyingPower`; the deposit separ-credit→credit→`settleDebt` waterfall and the freeze lender-borrowing loop are duplicated here. `dbsAccountNumber` is a new field stamped onto every event.
- **To finish the migration (gap list):** (1) call `actor.WalletActor.initSharding` from `Main` and retire `wallet.WalletActor` (mind the persistence-id namespace); (2) register the new command/event/state records in the Fastjson2 `ManifestRegistry` with new versioned manifests (serializer id `700001` stays immutable); (3) build the new projection for the new event types (the old projections were removed — see §3); (4) **add tests** — nothing yet covers `actor.WalletAggregate`/`actor.WalletActor`.

### 2. WalletFacade (`core/.../wallet/`) — the entity entry point (writes + single-wallet reads)
- **ask** (mutations needing confirmation, 10s timeout → `BusinessException(5000)`; `Rejected` → `BusinessException(code)`): `deposit`/`Withdraw`/`freeze`/`unfreeze`/`spend`/`freezeForT0`/`spendT0`/`addCredit`/`createWallet`.
- **ask** (single-wallet reads, strongly consistent — the entity's in-memory `WalletState` is authoritative; `GetWallet`→`WalletSnapshot`, `GetBuyingPower`→`BuyingPowerResult` computed inside the entity; `WALLET_NOT_EXIST`→`Rejected(4001)`): `getWallet`/`getBuyingPower`. Used by the read-your-wallet flows (user HTTP `GET /v1/user/wallet`, bridge REST `GET /v1/bridge/wallet/account-number/{acct}`, bridge gRPC `getWallet`/`getBuyingPower`).
- **tell** (fire-and-forget fan-out): `reconcileFromRayan`, `seedFromLegacy`. (`chargeSeparCredits` / `settleSeparCredits` exist as **commented-out** batch helpers in `WalletFacade`; the `ChargeSeparCredit` / `SettleSeparCredit` commands still exist in the protocol and are handled by the actor, but the facade doesn't fan them out today.)
- **bulk/list reads stay on the read-model tables (currently frozen — projections removed)**: `WalletQueryService`/`WalletRepository` still serve `findAll`, `getSeparCreditDebtorUsers`, transaction/turnover/credit-history, and the admin credit-history + turnover-cron wallet lookups (they need SQL and can't be served one-entity-per-account).

### 3. Read side (projections REMOVED — read model frozen)
The old CQRS projections were **removed** while the aggregate migrates to `ir.ebb.wallet.actor.WalletActor` (granular delta events); a new projection for that actor will be built separately. Deleted: `ProjectionBootstrap` + `WalletKafkaProjection` (`wallet-app/.../infrastructure/projection/`), `WalletReadModelProjection` and `WalletTags` (`core/.../wallet/`), the old actor's `tagsFor` override, the `pekko.projection.*` config block, the `pekko-projection` Gradle dependencies, and the legacy `V003` projection offset-store changeset (`pekko-projection-postgres.sql`). Until the new projection lands:
- The `wallet` / `wallet_debt` / `wallet_transaction` tables and the read-side services stay in place but are **frozen** — no new rows; bulk/list reads and the turnover-notify + Rayan-sync crons that read `WalletQueryService` see stale data.
- `wallet.state.updated` is no longer published; `wallet.turnover.notification` (cron → `KafkaWalletProducer`) still is. `KafkaWalletProducer` stays.
- The `pekko.projection_*` R2DBC tables (V005) stay for reuse by the new projection.

Read side today: `WalletQueryService` + read-only `WalletRepository` + `WalletTransactionQueryService` / `TurnoverQueryService` / `CreditHistoryQueryService` read those tables via **blocking JDBC over HikariCP** (`ir.ebb.base.jdbc.Jdbc`) — a separate pool from the R2DBC journal. **`WalletRepository` is read-only** (write methods deleted).

### 4. Serialization — Fastjson2
All `WalletCommand`/`WalletEvent`/`WalletReply`/`WalletState` implement `wallet.WalletSerializable`, bound **once** in `application.conf` to the custom `ir.ebb.wallet.serialization.FastJsonSerializer` (serializer id `700001`, **immutable forever** — Pekko stores it in every journal row/envelope and uses it, not the manifest, to pick the deserializer). It extends `SerializerWithStringManifest`: the manifest is an **explicit, versioned string literal** (`wallet-state:v1`, `cmd-deposit:v1`, …) from an immutable `ManifestRegistry` (`manifest↔class` is a `Map.get`, **never a Java class name**; unknown manifests throw `SerializationException`). Payloads are UTF-8 JSON via `JSON.toJSONBytes`/`JSON.parseObject`; the read context enables **none** of `SupportAutoType`/`SupportClassForName`/`FieldBased`, so no class is ever loaded from wire data (any stray `@type` is ignored); enums serialize by `.name()`. `ActorRef` (in ask commands) round-trips via Pekko's `ActorRefResolver` through custom Fastjson2 `ObjectWriter`/`ObjectReader`, registered as an `ObjectWriterModule`/`ObjectReaderModule` matching any `ActorRef`-assignable class (Fastjson2 resolves a field value's writer/reader by the value's runtime class, not the declared field type, so a plain `register(ActorRef.class, …)` silently misses) — it emits the same canonical serialization-format string Pekko's own Jackson module does. Schema evolution is an immutable `Migration` chain (`vN→vN+1`, walked at deserialize time; added fields ignored, missing fields default). Nested value types (`Tier`/`Debt`/`User`/`WalletTransaction`/`BuyingPower`) ride along as fields via Fastjson2's record/POJO codecs and are **not** registered. Pekko's built-in `jackson-json`/`jackson-cbor` (ids 30/31) stay registered for legacy/other types but are **no longer bound** to wallet messages. Verified by `FastJsonSerializerTest` + `FastJsonSerializerIntegrationTest` (through the real `SerializationExtension`) and `WalletActorTest` (testkit round-trips persisted events + snapshots + recovery). **Rolling upgrades across the Jackson→Fastjson2 boundary are NOT possible** (id change) — it is a hard cutover; safe here because the journal is fresh post-R2DBC-cutover + `--seed-from-legacy`.

### 5. Cluster + Bootstrap (`wallet-app/.../app/Main` + `app/di/` + `application.conf`)
Wiring is **Dagger 2** (`ir.ebb.wallet.app.di.WalletComponent` + 8 modules + 3 JWT qualifiers; dagger-compiler runs **only** in `wallet-app`, library modules carry `implementation libs.dagger` for the `javax.inject` annotations only — Dagger reads their `@Inject` constructors from the binary classpath). Injectable classes use `@Singleton` + `@RequiredArgsConstructor(onConstructor_ = @Inject)` (or explicit `@Inject` ctors); anything with a config primitive in its constructor (DataSource, ActorSystem, `KafkaWalletProducer`, 3 qualified `JwtVerifier`s, `GrpcServer`, `AdminWalletWebServiceImpl`'s boolean, `TurnoverNotifyWebServiceImpl`'s topic, `WalletJobs`' cron strings, Rayan http/login) is built in a `@Provides` method that reads Typesafe `Config` directly — there are **no `@Named`/String bindings**. Liquibase runs **inside** the `DataSource` `@Provides` (structural migrate-before-everything guarantee); `RayanResult.configure` runs inside the `RayanLoginService` `@Provides`. `Main` does **no** construction — it is the lifecycle orchestrator: `DaggerWalletComponent.builder().build()` → `dataSource()` (forces Liquibase first) → `ActorSystem` with a `Behaviors.ignore()` root (no guardian/registry actor) → `PekkoManagement.start()` (:8558) → `ClusterBootstrap.start()` (kubernetes-api discovery, service `wallet-service`) → `ClusterSharding.init(Entity.of(WalletActor.ENTITY_TYPE_KEY, WalletActor::create).withRole("wallet"))` → `walletDbProjection().init()` → (`--seed-from-legacy` via `LegacySeeder.run`) → HTTP bind (:8080) → `grpcServer().start()` (:9090) → `walletJobs().scheduleAll()` → shutdown hook (unbind → grpc stop → system terminate → kafka close → Hikari close) → `getWhenTerminated()`. `-parameters` is a **javac compiler flag** (set on every `JavaCompile` task in the root `build.gradle`, for Jackson's `ParameterNamesModule`); the only **runtime** CLI arg is `--seed-from-legacy` (one-off idempotent legacy import). Dagger graph validation happens at compile time in `:wallet-app:compileJava`; tests never build the component.

Three JWT audiences (Pekko HTTP directives + nimbus): User (`/v1/user/**`), Admin (`/v1/admin/**`), Bridge (`/v1/bridge/**` REST + gRPC `:9090`). Authorities claim, `PERMISSION_*` prefix.

### 6. External / scheduled
- **Rayan gateway** (`external/.../rayan/**`): Pekko HTTP client (`RayanHttpClient`), COPY bulk sync into `rayan_wallet` (external cache, NOT wallet state), login token cache. The Rayan sync job reconciles sharded wallet entities toward Rayan snapshots via `WalletFacade.reconcileFromRayan`.
- **Scheduled jobs** (`app/web/CronScheduler` + `WalletJobs`): Pekko Scheduler + cron-utils, 4 crons (Rayan sync/backup/remove, turnover-notify). Single-node (no distributed lock).
- **Kafka** (`app/infra/KafkaWalletProducer`): raw kafka-clients producer; `wallet.turnover.notification` (turnover-notify job). The `wallet.state.updated` projection was removed.

## Conventions

- **Errors**: throw `ApplicationException(ExceptionConstants.X)` inside the aggregate/entity; the entity maps it to `WalletReply.Rejected(code)`; `WalletFacade` maps `Rejected` → `BusinessException(code)` for HTTP/gRPC. Codes in `base/.../exception/ExceptionConstants` (4001 wallet-not-exist, 4005 insufficient-balance, 4007 duplicate-tracking-id, 5000 internal…). Add new codes there.
- **DTO layering**: each audience has its own `dto/request` + `dto/response` + `transformer` packages. Never leak entities/aggregates to controllers. `ResponseHelper` wraps `BaseResponse<T>` only; `PaginatedResponseDTO<T>` is constructed directly inside the web services (not via `ResponseHelper`).
- **Persistence**: schema changes go in a new Liquibase changeset under `wallet-app/src/main/resources/db/changelog/changesets/` (versioned `V00X__description.xml`), included from `db.changelog-master.xml`. The R2DBC journal/snapshot/projection tables live in the `pekko` schema, created by `V005__pekko_r2dbc.xml` (Postgres DDL from the plugin docs). The legacy `pekko-persistence-jdbc` journal tables (`V003` changeSet 10, `public` schema) are dead DDL (their drop changeset was itself removed from the repo); the legacy projection offset-store changeset (`V003` changeSet 11 + `pekko-projection-postgres.sql`) was deleted with the projections. `Money` wraps `Long` (rials — integer longs, no decimals).
- **credit_history** is a direct-write audit (admin metadata not in events); **turnover** is currently a direct write (not yet projection-fed).
- **What's gone**: `WalletCommandService(+Impl)`, `WalletRepository` writes, `OptimisticLockingFailureException`, `spendAndTransfer`/`TRANSFER` — do not reintroduce them. Also gone from persistence: `pekko-persistence-jdbc`, Slick, `JdbcProjection`/`JdbcHandler`/`JdbcSession`, `HikariJdbcSession`, the `eventsByTag` 16-tag scheme, and the one-off `pekko-persistence-r2dbc-migration` tooling (removed after the JDBC→R2DBC cutover). Also removed: the whole old projection stack — `ProjectionBootstrap`, `WalletReadModelProjection`, `WalletKafkaProjection`, `WalletTags`/`tagsFor`, the `pekko-projection` Gradle dependencies and the `pekko.projection.*` config — do not reintroduce them; they come back only with the new projection for `ir.ebb.wallet.actor.WalletActor`.
- Prefer **fire-and-forget `tell`** over `ask` where a reply isn't required; immutable messages; no shared mutable state.
