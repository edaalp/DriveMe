package com.driveme.backend.entity;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * Passenger user type.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper=true)
public class Passenger extends BaseUser {

    public Passenger() {
        super();
    }
}
