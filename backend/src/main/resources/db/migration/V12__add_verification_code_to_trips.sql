-- Adds a short 4-digit code used to verify that the passenger has boarded the
-- vehicle. The driver shares this code verbally; the passenger enters / confirms
-- it in the app to transition the trip from DRIVER_ARRIVED → IN_PROGRESS.

ALTER TABLE trips
    ADD COLUMN IF NOT EXISTS verification_code VARCHAR(10);
