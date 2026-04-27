package com.driveme.backend.common;

/**
 * Lifecycle status of a {@code Trip}.
 *
 * <p>A trip is created in {@link #ACCEPTED} the moment a driver accepts a
 * {@code TripRequest}. It then progresses forward through the states below
 * and terminates in one of the {@code CANCELLED_*}, {@link #NO_SHOW} or
 * {@link #COMPLETED} states. Invalid transitions are rejected inside
 * {@code Trip} entity helper methods.
 */
public enum TripStatus {

    /** Driver accepted the request and is heading to the pickup location. */
    ACCEPTED,

    /** Driver is physically at the pickup point and waiting for the passenger. */
    DRIVER_ARRIVED,

    /** Passenger is onboard and the ride is actively underway. */
    IN_PROGRESS,

    /** Ride finished successfully; final payment/rating may still be pending. */
    COMPLETED,

    /** Driver cancelled the trip before it started. Usually incurs a penalty fee. */
    CANCELLED_BY_DRIVER,

    /** Passenger cancelled the trip before it started. May incur a penalty fee. */
    CANCELLED_BY_PASSENGER,

    /** Passenger did not show up at pickup within the grace window. */
    NO_SHOW
}
