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
public class TripStartResponse {
    private Long tripId;
    private Long bookingId;
    private Long ambulanceId;
    private Long driverId;
    private LocalDateTime startTime;
    private String status;
}
