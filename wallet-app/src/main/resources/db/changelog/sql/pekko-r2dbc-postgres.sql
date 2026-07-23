-- pekko-persistence-r2dbc + pekko-projection-r2dbc schema (Postgres).
-- Tables live in a dedicated `pekko` schema so they can coexist with the legacy
-- pekko-persistence-jdbc tables (public.event_journal / public.event_tag / public.snapshot)
-- during the one-off JDBC->R2DBC journal migration. The legacy tables are dropped in a later
-- changeset once the migration is verified. DDL verbatim from the pekko-persistence-r2dbc
-- "Creating the schema" reference (Postgres variant); durable_state omitted (no DurableStateBehavior).
CREATE SCHEMA IF NOT EXISTS pekko;

CREATE TABLE IF NOT EXISTS pekko.event_journal (
  slice INT NOT NULL,
  entity_type VARCHAR(255) NOT NULL,
  persistence_id VARCHAR(255) NOT NULL,
  seq_nr BIGINT NOT NULL,
  db_timestamp timestamp with time zone NOT NULL,

  event_ser_id INTEGER NOT NULL,
  event_ser_manifest VARCHAR(255) NOT NULL,
  event_payload BYTEA NOT NULL,

  deleted BOOLEAN DEFAULT FALSE NOT NULL,
  writer VARCHAR(255) NOT NULL,
  adapter_manifest VARCHAR(255),
  tags TEXT[],

  meta_ser_id INTEGER,
  meta_ser_manifest VARCHAR(255),
  meta_payload BYTEA,

  PRIMARY KEY (persistence_id, seq_nr)
);

CREATE INDEX IF NOT EXISTS pekko.event_journal_slice_idx
    ON pekko.event_journal (slice, entity_type, db_timestamp, seq_nr);

CREATE TABLE IF NOT EXISTS pekko.snapshot (
  slice INT NOT NULL,
  entity_type VARCHAR(255) NOT NULL,
  persistence_id VARCHAR(255) NOT NULL,
  seq_nr BIGINT NOT NULL,
  write_timestamp BIGINT NOT NULL,
  ser_id INTEGER NOT NULL,
  ser_manifest VARCHAR(255) NOT NULL,
  snapshot BYTEA NOT NULL,
  meta_ser_id INTEGER,
  meta_ser_manifest VARCHAR(255),
  meta_payload BYTEA,

  PRIMARY KEY (persistence_id)
);

-- Primitive offset types (optional for eventsBySlices, which mainly uses the timestamp table).
CREATE TABLE IF NOT EXISTS pekko.projection_offset_store (
  projection_name VARCHAR(255) NOT NULL,
  projection_key VARCHAR(255) NOT NULL,
  current_offset VARCHAR(255) NOT NULL,
  manifest VARCHAR(32) NOT NULL,
  mergeable BOOLEAN NOT NULL,
  last_updated BIGINT NOT NULL,
  PRIMARY KEY (projection_name, projection_key)
);

-- Timestamp offsets used by eventsBySlices projections.
CREATE TABLE IF NOT EXISTS pekko.projection_timestamp_offset_store (
  projection_name VARCHAR(255) NOT NULL,
  projection_key VARCHAR(255) NOT NULL,
  slice INT NOT NULL,
  persistence_id VARCHAR(255) NOT NULL,
  seq_nr BIGINT NOT NULL,
  -- timestamp_offset is the db_timestamp of the original event
  timestamp_offset timestamp with time zone NOT NULL,
  -- timestamp_consumed is when the offset was stored; consumer lag = timestamp_consumed - timestamp_offset
  timestamp_consumed timestamp with time zone NOT NULL,
  PRIMARY KEY (slice, projection_name, timestamp_offset, persistence_id, seq_nr)
);

CREATE TABLE IF NOT EXISTS pekko.projection_management (
  projection_name VARCHAR(255) NOT NULL,
  projection_key VARCHAR(255) NOT NULL,
  paused BOOLEAN NOT NULL,
  last_updated BIGINT NOT NULL,
  PRIMARY KEY (projection_name, projection_key)
);
