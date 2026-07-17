-- Official pekko-projection-jdbc 1.1.0 Postgres schema (verbatim from the source tree:
-- examples/src/test/resources/create-table-postgres.sql). Lowercase identifiers match
-- pekko.projection.jdbc.offset-store.use-lowercase-schema = true (the plugin default).

CREATE TABLE IF NOT EXISTS pekko_projection_offset_store (
  projection_name VARCHAR(255) NOT NULL,
  projection_key VARCHAR(255) NOT NULL,
  current_offset VARCHAR(255) NOT NULL,
  manifest VARCHAR(4) NOT NULL,
  mergeable BOOLEAN NOT NULL,
  last_updated BIGINT NOT NULL,
  PRIMARY KEY(projection_name, projection_key)
);

CREATE INDEX IF NOT EXISTS projection_name_index ON pekko_projection_offset_store (projection_name);

CREATE TABLE IF NOT EXISTS pekko_projection_management (
  projection_name VARCHAR(255) NOT NULL,
  projection_key VARCHAR(255) NOT NULL,
  paused BOOLEAN NOT NULL,
  last_updated BIGINT NOT NULL,
  PRIMARY KEY(projection_name, projection_key)
);
