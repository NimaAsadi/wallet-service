-- Post-cutover cleanup: drop the legacy pekko-persistence-jdbc tables (public schema) and the
-- migration tool's progress table. The R2DBC journal/snapshot/projection tables live in the
-- `pekko` schema (V005) and are NOT touched here.
--
-- ⚠ Apply ONLY after the JDBC->R2DBC journal migration has completed and been verified
-- (events were streamed public.event_journal -> pekko.event_journal). These DROPs are
-- irreversible. IF EXISTS keeps this safe on fresh environments that never had the JDBC tables.
-- event_tag has an FK to event_journal, so drop it first.
DROP TABLE IF EXISTS public.event_tag;
DROP TABLE IF EXISTS public.event_journal;
DROP TABLE IF EXISTS public.snapshot;
DROP TABLE IF EXISTS public.pekko_projection_offset_store;
DROP TABLE IF EXISTS public.pekko_projection_management;
DROP TABLE IF EXISTS public.migration_progress;
