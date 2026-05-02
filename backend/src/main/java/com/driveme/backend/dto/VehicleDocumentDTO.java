package com.driveme.backend.dto;

import com.driveme.backend.common.VehicleDocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDocumentDTO {

    private UUID id;
    private String fileName;
    private VehicleDocumentType documentType;
}
