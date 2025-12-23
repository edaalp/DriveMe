package com.driveme.backend.entity;

import com.driveme.backend.common.BaseEntity;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Abstract user with shared identity and contact fields.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper=true)
@AllArgsConstructor
public abstract class BaseUser extends BaseEntity {

    private String email;
    
    private String fullName;

    private String userName;

    private String phoneNumber;

    private boolean active = true;

    private String passwordHash; 

    protected BaseUser() {
        // for JPA
    }
}
