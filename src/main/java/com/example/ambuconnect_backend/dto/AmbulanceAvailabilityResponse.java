package com.example.ambuconnect_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmbulanceAvailabilityResponse {
    private boolean success;
    private String message;
    private Long id;
    private Long ambulanceId;
    private Boolean isAvailable;
    private LocalDateTime updatedAt;
}
