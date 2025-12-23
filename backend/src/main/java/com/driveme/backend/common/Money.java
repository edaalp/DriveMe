package com.driveme.backend.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Simple embeddable Money value object.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Money {

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    /**
     * Creates Money with TRY currency.
     */
    public static Money ofTRY(BigDecimal amount) {
        return new Money(amount, "TRY");
    }

    /**
     * Creates Money with TRY currency.
     */
    public static Money ofTRY(double amount) {
        return new Money(BigDecimal.valueOf(amount), "TRY");
    }
}
