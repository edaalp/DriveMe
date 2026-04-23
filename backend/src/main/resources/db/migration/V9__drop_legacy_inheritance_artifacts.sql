-- =====================================================================
-- V9 — Drop legacy inheritance artifacts
-- =====================================================================
--
-- WHY:
--   Earlier versions of this codebase used JOINED-style JPA inheritance
--   for user types. That created separate tables `passenger`, `drivers`,
--   `admins`, `vehicle`, `users`, `messages` and matching FK constraints
--   on `trip_requests`, `driver_locations`, `driver_trip_rejections`.
--
--   The current codebase uses SINGLE_TABLE inheritance: every Passenger,
--   Driver and Admin row lives in `base_user` (with a `dtype`
--   discriminator). The legacy tables are now empty (or hold stale
--   orphan rows) but the FOREIGN KEY constraints on `trip_requests`
--   etc. still point to them — so every INSERT into `trip_requests`
--   fails with:
--
--     ERROR: insert or update on table "trip_requests" violates
--            foreign key constraint "fk5oabsxalgt1oi5ffwvxv41myx"
--     Detail: Key (passenger_id)=(<uuid>) is not present in
--             table "passenger".
--
--   `ddl-auto: update` only ADDS, never DROPS — so this cleanup must be
--   done explicitly via a migration.
--
-- WHAT IT DOES (idempotent):
--   1. Drops the legacy FOREIGN KEY constraints that still point at the
--      old per-type tables.
--   2. Drops the legacy / orphan tables themselves so future
--      `ddl-auto` cycles can't re-attach broken FKs to them.
--
--   The "current" FKs (→ base_user, → vehicles, → trip_requests) are
--   left untouched.
-- =====================================================================

-- 1) Drop the legacy FOREIGN KEY constraints ----------------------------
ALTER TABLE IF EXISTS trip_requests
    DROP CONSTRAINT IF EXISTS fk5oabsxalgt1oi5ffwvxv41myx;     -- passenger_id  -> passenger(id)

ALTER TABLE IF EXISTS trip_requests
    DROP CONSTRAINT IF EXISTS fkau2ppm7u2im3dyh83xue0ac2g;     -- matched_driver_id -> drivers(id)

ALTER TABLE IF EXISTS driver_locations
    DROP CONSTRAINT IF EXISTS fk8cqncho6pk341r6r0h6lc332b;     -- driver_id -> drivers(id)

ALTER TABLE IF EXISTS driver_trip_rejections
    DROP CONSTRAINT IF EXISTS fkpwnc430do1tnhyhh6eto7q88s;     -- driver_id -> drivers(id)

-- 2) Drop the legacy / orphan tables (CASCADE removes any constraints
--    we may have missed, e.g. the legacy `passenger.id -> base_user.id`
--    and `vehicle.driver_id -> base_user.id`). ---------------------------
DROP TABLE IF EXISTS messages   CASCADE;
DROP TABLE IF EXISTS users      CASCADE;
DROP TABLE IF EXISTS vehicle    CASCADE;
DROP TABLE IF EXISTS passenger  CASCADE;
DROP TABLE IF EXISTS drivers    CASCADE;
DROP TABLE IF EXISTS admins     CASCADE;
