package com.driveme.backend.dto;

import com.driveme.backend.common.TransmissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new vehicle.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVehicleRequest {

    @NotBlank(message = "Plate number is required")
    @Size(max = 20, message = "Plate number must be at most 20 characters")
    private String plateNumber;

    @NotBlank(message = "Brand is required")
    @Size(max = 50, message = "Brand must be at most 50 characters")
    private String brand;

    @NotBlank(message = "Model is required")
    @Size(max = 50, message = "Model must be at most 50 characters")
    private String model;

    @NotNull(message = "Year is required")
    private Integer year;

    @NotNull(message = "Transmission type is required")
    private TransmissionType transmission;
}

