package com.driveme.backend.payment;

import com.driveme.backend.payment.dto.PaymentResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting Payment entities to DTOs.
 */
@Component
public class PaymentMapper {

    /**
     * Converts a Payment entity to a PaymentResponse DTO.
     *
     * @param payment the payment entity
     * @return the payment response DTO
     */
    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .tripId(payment.getTripId())
                .userId(payment.getUserId())
                .amount(payment.getPaymentAmount() != null ? payment.getPaymentAmount().getAmount() : null)
                .currency(payment.getPaymentAmount() != null ? payment.getPaymentAmount().getCurrency() : null)
                .method(payment.getMethod())
                .status(payment.getStatus())
                .dueBy(payment.getDueBy())
                .confirmedAt(payment.getConfirmedAt())
                .description(payment.getDescription())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .overdue(payment.isOverdue())
                .build();
    }
}

