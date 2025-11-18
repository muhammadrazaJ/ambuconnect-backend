package com.example.ambuconnect_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmbulanceResponse {
    private boolean success;
    private String message;
    private Long id;
    private String vehicleNumber;
    private String model;
    private String ambulanceType;
    private Boolean availability;
}
