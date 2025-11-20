package com.example.ambuconnect_backend.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAmbulanceAvailabilityRequest {
    @NotNull
    private Boolean isAvailable;
}
