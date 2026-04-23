-- ──────────────────────────────────────────────────────────────────────────
-- V11: Rating aggregates shared by Driver + Passenger.
--
-- `avg_rating` already existed as a primitive double on Driver (Hibernate
-- auto-created the column via ddl-auto=update). We are widening the
-- semantics: the column is now shared with Passenger (moved up to
-- BaseUser) so both sides of a completed trip can hold an aggregated
-- rating. `rating_count` is introduced as the new supporting column so
-- we can compute running averages without scanning every trip.
--
-- IF NOT EXISTS guards make this idempotent on environments where the
-- Driver column was already present.
-- ──────────────────────────────────────────────────────────────────────────

ALTER TABLE base_user
    ADD COLUMN IF NOT EXISTS avg_rating   DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS rating_count INTEGER;

-- Hibernate originally created `avg_rating` as NOT NULL because the legacy
-- field was a Java primitive `double`. Now that it's been lifted to
-- BaseUser and typed as `Double` (nullable), we must relax the constraint
-- so Passenger rows can insert without a rating.
ALTER TABLE base_user
    ALTER COLUMN avg_rating DROP NOT NULL;
