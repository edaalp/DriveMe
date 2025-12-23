package com.driveme.backend.helper;

import com.driveme.backend.dto.PenaltyResponse;
import com.driveme.backend.entity.Penalty;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting Penalty entities to DTOs.
 */
@Component
public class PenaltyMapper {

    /**
     * Converts a Penalty entity to a PenaltyResponse DTO.
     *
     * @param penalty the penalty entity
     * @return the penalty response DTO
     */
    public PenaltyResponse toResponse(Penalty penalty) {
        if (penalty == null) {
            return null;
        }

        return PenaltyResponse.builder()
                .id(penalty.getId())
                .userId(penalty.getUserId())
                .type(penalty.getType())
                .amount(penalty.getPenaltyAmount() != null ? penalty.getPenaltyAmount().getAmount() : null)
                .currency(penalty.getPenaltyAmount() != null ? penalty.getPenaltyAmount().getCurrency() : null)
                .reason(penalty.getReason())
                .paid(penalty.isPaid())
                .tripId(penalty.getTripId())
                .paymentId(penalty.getPaymentId())
                .createdAt(penalty.getCreatedAt())
                .updatedAt(penalty.getUpdatedAt())
                .build();
    }
}

