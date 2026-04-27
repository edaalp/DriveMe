-- ──────────────────────────────────────────────────────────────────────────
-- V10: `trips` — confirmed rides (post-acceptance lifecycle artifact).
--
-- A Trip is created the moment a driver accepts a pending TripRequest. It
-- snapshots pickup/destination/price/preference/vehicle so that later
-- deletions on the source TripRequest (or on the chosen vehicle) do not
-- corrupt the driver's or passenger's ride history.
--
-- The schema is intentionally conservative (no FK CASCADE on delete for
-- passenger/driver) so that accidental user deletion preserves trips for
-- audit. Hibernate's validator tolerates extra columns here.
-- ──────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS trips (
    id                       UUID           PRIMARY KEY,
    created_at               TIMESTAMPTZ    NOT NULL,
    updated_at               TIMESTAMPTZ    NOT NULL,
    version                  BIGINT         NOT NULL,

    -- Relationships
    trip_request_id          UUID           NOT NULL UNIQUE,
    driver_id                UUID           NOT NULL,
    passenger_id             UUID           NOT NULL,
    vehicle_id               UUID,

    -- Route (snapshot)
    pickup_lat               DOUBLE PRECISION,
    pickup_lon               DOUBLE PRECISION,
    pickup_address           VARCHAR(500),
    destination_lat          DOUBLE PRECISION,
    destination_lon          DOUBLE PRECISION,
    destination_address      VARCHAR(500),

    planned_distance_km      DOUBLE PRECISION,
    planned_duration_minutes INTEGER,
    actual_distance_km       DOUBLE PRECISION,
    actual_duration_minutes  INTEGER,
    route_polyline           VARCHAR(4096),

    -- Pricing & payment
    agreed_amount            NUMERIC(19, 2),
    agreed_currency          VARCHAR(3),
    final_amount             NUMERIC(19, 2),
    final_currency           VARCHAR(3),
    penalty_amount           NUMERIC(19, 2),
    penalty_currency         VARCHAR(3),
    payment_status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    payment_method           VARCHAR(20),

    -- Preferences (snapshot)
    with_pet                 BOOLEAN        NOT NULL DEFAULT FALSE,

    -- Lifecycle
    status                   VARCHAR(30)    NOT NULL DEFAULT 'ACCEPTED',
    accepted_at              TIMESTAMPTZ    NOT NULL,
    driver_arrived_at        TIMESTAMPTZ,
    started_at               TIMESTAMPTZ,
    completed_at             TIMESTAMPTZ,
    cancelled_at             TIMESTAMPTZ,
    cancellation_reason      VARCHAR(500),

    -- Live tracking
    driver_last_lat          DOUBLE PRECISION,
    driver_last_lon          DOUBLE PRECISION,
    eta_to_pickup_minutes    INTEGER,

    -- Ratings & feedback
    rating_by_passenger      INTEGER,
    rating_by_driver         INTEGER,
    comment_by_passenger     VARCHAR(500),
    comment_by_driver        VARCHAR(500),

    CONSTRAINT fk_trips_trip_request
        FOREIGN KEY (trip_request_id) REFERENCES trip_requests (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_trips_driver
        FOREIGN KEY (driver_id) REFERENCES base_user (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_trips_passenger
        FOREIGN KEY (passenger_id) REFERENCES base_user (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_trips_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles (id)
        ON DELETE SET NULL,

    CONSTRAINT ck_trips_rating_passenger
        CHECK (rating_by_passenger IS NULL OR rating_by_passenger BETWEEN 1 AND 5),
    CONSTRAINT ck_trips_rating_driver
        CHECK (rating_by_driver    IS NULL OR rating_by_driver    BETWEEN 1 AND 5)
);

CREATE INDEX IF NOT EXISTS ix_trips_driver_status    ON trips (driver_id, status);
CREATE INDEX IF NOT EXISTS ix_trips_passenger_status ON trips (passenger_id, status);
CREATE INDEX IF NOT EXISTS ix_trips_status           ON trips (status);
