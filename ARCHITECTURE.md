# Wallet-Service Architecture

Production-grade **Apache Pekko + DDD + Event Sourcing + CQRS + Cluster Sharding** wallet/ledger
service. The wallet is an event-sourced, cluster-sharded aggregate; the **event journal is the
single source of truth**; single-wallet reads (`getWallet`/`getBuyingPower`) are served **strongly
consistent from the entity**, while bulk/list reads are served by read-model tables (currently **frozen** — the CQRS
projections were removed pending a new projection for the next-gen actor); the cluster
runs on Kubernetes via Cluster Bootstrap. **Java 21, no Spring, no JPA.**

---

## 1. Architecture diagram (text)

```
            ┌────────── Pekko Cluster (K8s — Cluster Bootstrap via kubernetes-api) ──────────
            │
            │  HTTP :8080  (Pekko HTTP, JWT: user / admin / bridge)    gRPC :9090 (grpc-java, bridge)
            │
            │   writes ──────────────► WalletFacade ─► Cluster Sharding (entity type "wallet", role "wallet")
            │   single-wallet GETs ──► WalletFacade ─► sharded entity (strongly consistent)
            │   bulk / list GETs ────► read-side query layer (JDBC / HikariCP; tables frozen — projections removed)
            │
            │   WalletFacade ─► sharding.entityRefFor("wallet", accountNumber)
            │        ask (mutations, 10s) | tell (Rayan reconcile, legacy seed, internal Passivate)
            │             ▼
            │   WalletActor = EventSourcedBehavior<WalletCommand, WalletEvent, WalletState>  (1 per account)
            │        CommandHandler: validate → state.toAggregate() → reuse Wallet cascade → Effect().persist
            │        EventHandler:   state = event.resultingState() (idempotent)   snapshot every 100 events
            │        passivation (receive-timeout → stop)   NO DB I/O inside
            │             ▼
            │   pekko-persistence-r2dbc ─► Postgres — `pekko` schema (event_journal + snapshot)
            │   (the ONLY write path to wallet-domain state)
            └───────────────────────────────────────────────────────────────────────────────
```

Source of truth = the **`pekko.event_journal`** table. The old version-column optimistic lock + reload-retry was
replaced by per-persistence-id journal serialization (strictly stronger).

---

## 2. Message flow

**Write (single wallet, confirmation needed) — ask:**
```
HTTP/gRPC → WalletFacade.deposit(trackingId, user, amount, delay, type)
         → sharding.entityRefFor("wallet", accountNumber).ask(Deposit(..., replyTo))   [10s timeout]
         → WalletActor commandHandler: validate → toAggregate() → wallet.deposit() → persist WalletMutated → thenReply Accepted(state)
         → facade maps Accepted → Wallet, or Rejected(code) → BusinessException(code)
```
**Write (fan-out, fire-and-forget) — tell:** Rayan reconcile and legacy seed (plus the internal
`Passivate`) are one `tell` per wallet (no reply). No ask = no blocking. (`ChargeSeparCredit` /
`SettleSeparCredit` commands remain in the protocol but aren't fanned out by the facade today.)
**Read (single wallet, strongly consistent) — ask:** `getWallet` / `getBuyingPower` are `ask`-ed
against the entity — its in-memory `WalletState` is authoritative (`WALLET_NOT_EXIST → Rejected(4001)`).
**Read (bulk / list) — read-model tables (currently frozen):** `findAll`, transaction / turnover / credit-history,
separ-credit debtor scans, etc. bypass the entity → `WalletQueryService` reads the read-model
tables directly (they need SQL and can't be served one-entity-per-account); the projections
that fed them were removed pending the new projection for the next-gen actor.

---

## 3. Cluster topology

- **Cluster** formed by **Cluster Bootstrap** using the **`kubernetes-api`** discovery method
  (pods labeled `app=wallet-service`, headless Service `wallet-service`).
- **Management HTTP** on `:8558` (Pekko Management) for bootstrap contact points + operational queries.
- **Artery remoting** on `:25520` (`PEKKO_CANONICAL_HOSTNAME` ← pod IP via downward API).
- **Sharding** entity type `"wallet"`, role `"wallet"`; `EntityId = accountNumber` (1 entity per wallet).
- Postgres holds both: the journal/snapshot tables in the **`pekko` schema** (one R2DBC
  connection-factory shared by journal + snapshot) and the read-model tables (`wallet`,
  `wallet_debt`, `wallet_transaction`, …) in `public` (read via a separate HikariCP JDBC pool, ~80
  conns; currently frozen — the projections were removed pending the new one). `replicas=1` is a valid single-member cluster; `≥2` exercises shard rebalance.

---

## 4. Event flow

```
Command ─► WalletActor (validate) ─► Effect().persist(WalletEvent)
   persisted (Fastjson2) to the `pekko.event_journal` table
   EventHandler applies: state = event.resultingState()   (absolute values → idempotent)
   snapshot every 100 events (keep 2) in `pekko.snapshot`
```
Events: `WalletCreated(state)` · `WalletMutated(legs, resultingState)` · `WalletSeeded(state)`
(migration seed). Each carries the **full resulting state**, so re-applying is a no-op.

---

## 5. Projection flow (REMOVED)

The CQRS projections were removed while the aggregate migrates to `ir.ebb.wallet.actor.WalletActor`
(granular delta events): `ProjectionBootstrap`, `WalletReadModelProjection` (read model),
`WalletKafkaProjection` (Kafka), the `pekko.projection.*` config and the `pekko-projection` Gradle
dependencies are all gone, along with `WalletTags`/`tagsFor` on the old actor. A new projection for
the new actor will be built separately — same read-model tables (`wallet`, `wallet_debt`,
`wallet_transaction`), `eventsBySlices` over the R2DBC read journal, offset + read model committed in
one R2DBC transaction (`exactlyOnce`), deterministic transaction ids for idempotent replay. The
`pekko.projection_*` tables from `V005__pekko_r2dbc.xml` remain in place for reuse. Until the new
projection lands, the read-model tables are frozen and `wallet.state.updated` is not published.

---

## 6. Removed components

| Removed | Replaced by |
|---|---|
| `actor/WalletRegistryActor` (singleton registry root) | Cluster Sharding (sole entity-lookup mechanism) |
| `actor/DailyWalletActor` (per-user-per-day actor) | Long-lived `WalletActor` (entityId = accountNumber, no date) |
| `actor/WalletActorService` (in-process facade) | `wallet/WalletFacade` (sharding ask/tell) |
| `actor/message/WalletCommand`, `WalletEvent` (duplicate protocol) | `wallet/WalletCommand`, `wallet/WalletEvent` |
| `service/command/WalletCommandService(+Impl)` (write side) | `WalletActor` + projections |
| `WalletRepository` write methods + UPDATE/INSERT SQL | `WalletReadModelProjection` (read-only repo now) |
| `OptimisticLockingFailureException` (version-column guard) | per-persistence-id journal serialization |
| `spendAndTransfer` + `WalletTransactionType.TRANSFER` | dropped (no caller; cross-wallet transfer would be a saga) |
| Spring / JPA / Hibernate / Spring Data | pure Pekko + plain JDBC read side |

### JDBC → R2DBC cutover (later rewrite)

| Removed | Replaced by |
|---|---|
| `pekko-persistence-jdbc` + `event_tag` (`eventsByTag`, 16 tags) | `pekko-persistence-r2dbc` + `eventsBySlices` (1024 slices / 16 ranges, single tag `"wallet"`) |
| Jackson **CBOR** serialization | Jackson **JSON** (`jackson-json` binding) |
| `JdbcProjection` / `JdbcHandler` / `JdbcSession` / `HikariJdbcSession` | `R2dbcProjection` / `R2dbcHandler` / `R2dbcSession` (shared R2DBC pool) |
| `public.event_journal` / `event_tag` / `snapshot` / offset tables | `pekko` schema (`event_journal`, `snapshot`, `projection_offset_store`, `projection_timestamp_offset_store`, `projection_management`) |
| one-off `pekko-persistence-r2dbc-migration` tooling | removed after cutover (snapshot-seed / replay instead) |

No hand-written write-side JDBC remains. The persistent entity performs **zero** DB I/O; all writes
land in Postgres via the R2DBC journal/projection plugins, and the read side is a separate HikariCP
JDBC pool.

---

## 7. New package structure

```
core/.../ir/ebb/wallet/
  wallet/                      # event-sourced domain (flat protocol)
    WalletSerializable         # serializer marker (Fastjson2, id 700001)
    WalletCommand, WalletEvent, WalletState, WalletReply
    WalletActor                # EventSourcedBehavior<WalletCommand,WalletEvent,WalletState>
    WalletFacade               # sharding ask + fire-and-forget tell
  aggregate/                   # WalletAggregate (next-gen event-sourced aggregate; NOT yet wired)
  actor/ (+ command/, event/)  # ir.ebb.wallet.actor.WalletActor + protocol (successor of wallet/; NOT yet wired)
  valueobject/                 # Wallet, WalletDebt, WalletTransaction (money-movement cascade, reused)
  repository/                  # READ-ONLY (WalletRepository, credit/transaction/turnover repos)
  service/query/               # WalletQueryService (+ transaction/turnover/credit queries)
  entity/, constant/, dto/     # JDBC read POJOs, enums, DTOs

wallet-app/.../ir/ebb/wallet/
  app/Main                     # composition root (Behaviors.ignore() root + wiring)
  app/web/                     # Pekko HTTP routes, JWT (3 realms), cron, gRPC server
  app/{user,bridge,admin}/...  # audience web services (facade + queries)
  app/infra/                   # HikariCP, Liquibase, KafkaWalletProducer
  infrastructure/
    migration/LegacySeeder     # --seed-from-legacy

base/   ir.ebb.base.* (jdbc, exceptions, security) + ir.ebb.common.* (re-homed DTOs/enums)
external/ ir.ebb.external.rayan.* (Pekko HTTP client + COPY sync, external cache)
user-info/ ir.ebb.userinfo.* (User read-model, plain JDBC)
```

---

## 8. Architectural improvements

1. **Event sourcing** — the wallet's state is the replay of persisted events; full audit history for free.
2. **No actor registry** — Cluster Sharding handles creation, lookup, routing, passivation, distribution, relocation, and recovery. One entity per wallet cluster-wide.
3. **Strict per-entity serialization** — the journal serializes writes per persistence-id, eliminating the optimistic-lock + reload-retry race.
4. **CQRS** — the write model (events) is decoupled from the read-model tables; bulk/list reads scale independently of the entities, while single-wallet reads go to the entity for strong consistency. (The projections that fed the read model were removed pending the new one; the tables are frozen.)
5. **Exactly-once read model (design kept for the new projection)** — offset + read-model writes share one R2DBC transaction (`R2dbcProjection.exactlyOnce`); idempotent under replay.
6. **Cluster-safe messaging** — Fastjson2 + `WalletSerializable` marker; verified by the persistence-testkit round-trip in `WalletActorTest`.
7. **Long-lived entity + passivation** — no per-day actors; idle entities passivate (memory-bounded) and recover from the journal on demand.
8. **Idempotent fire-and-forget** — duplicate trackingId rejection + absolute-state events make fan-out (Rayan reconcile, legacy seed) safe.
9. **No blocking JDBC in persistent actors** — the entity mutates in-memory (reused aggregate) and persists via the R2DBC journal.
10. **No Spring/JPA** — explicit composition root (`Main`); smaller surface, faster startup, no magic.

---

## 9. Remaining technical debt

- **SplitBrainResolver** not yet configured — for production multi-node, add SBR (lease strategy over a kubernetes/jdbc lease) instead of the default downing provider.
- **`turnover` read model is not projection-fed** — `REMAINING` rows are written directly by the Rayan sync job; a turnover projection (derived from `WalletMutated` legs) is a follow-up.
- **`credit_history` is a direct-write audit** (carries admin metadata not in events), intentionally not event-sourced.
- **Kafka `wallet.state.updated`** — removed with the projections; if it returns with the new projection, prefer a transactional publish over fire-and-forget.
- **`WalletClusterShardingTest`** — not yet written (needs Docker/CI); shard routing is unverified at the integration level (entity + aggregate + serialization are unit-tested in `WalletAggregateTest` + `WalletActorTest`). An integration test belongs to the new projection when it is built.
- **`-parameters` compiler flag** is on (helps Jackson ParameterNamesModule); keep it.
- **`wallet.version` column is vestigial** on the read model (no optimistic lock anymore); dropping it is an optional future cleanup.

---

## 10. Significant architectural decisions (rationale)

1. **EntityId = `accountNumber`** (not the UUID `walletId`, not a per-day composite). `accountNumber` is the stable, externally-addressable identity carried in every request, so it enables **direct sharding with no lookup hop**. Exactly one long-lived entity per wallet (no date in the key).
2. **No new daily-domain commands** (`CloseBusinessDay` etc.). Business behavior is unchanged; the entity is long-lived and the existing Rayan-reconcile settlement is preserved. The architecture supports adding daily commands later.
3. **Jackson JSON + `WalletSerializable` marker** (vs per-type serializers). One marker binding covers all protocol types; the concrete record type travels in the manifest (Pekko `type-in-manifest = on` default), so sealed interfaces need no `@JsonTypeInfo`; `ActorRef` is handled by the auto-registered `PekkoTypedJacksonModule`. (An earlier iteration used CBOR; it was moved to plain JSON during the R2DBC cutover for simpler debugging/interop.)
4. **`exactlyOnce` for a DB read model, `atLeastOnce` for Kafka** (rationale kept for the new projection). The DB read model is committed atomically with its offset in one R2DBC transaction (no duplicates); Kafka is inherently at-least-once, so its projection only advances the offset and publishes best-effort — keeping the DB read model clean. (The projections implementing this were removed; see §5.)
5. **Aggregate reuse over rewrite.** The intricate, already-correct `Wallet` cascade (settleDebt / applyFreeze / buyingPower waterfall) is reused verbatim as a transient scratchpad (`state.toAggregate()` → mutate → `WalletState.fromAggregate()`), avoiding re-deriving money-movement logic.
6. **Snapshot-seed + hard cutover** (`--seed-from-legacy`). Existing mutable rows become `WalletSeeded` events (idempotent; no-op on re-run). No dual-write complexity; reads stay consistent throughout because old and new share the projection-fed tables.
7. **`User` is a read-model, not an entity.** Only `Wallet` has behavioral invariants → only it is event-sourced. `user-info` stays a plain JDBC lookup (DDD: aggregates vs read models).
8. **`credit_history` as direct-write audit.** It carries admin metadata (`createdId`/`createdBy`/`errorMessage`) absent from the event stream; forcing it through events would lose that context, so it is written alongside the entity's `AddCredit`.
9. **Passivation via receive-timeout → `Effect().stop()`.** Canonical, version-proof; sharding recreates the entity from the journal on the next message. Keeps memory bounded across many wallets.
10. **No `spendAndTransfer`/`TRANSFER`.** No caller exists; a cross-wallet transfer cannot be atomic across two sharded entities and should be a saga if ever needed — not a hidden transactional shortcut.
