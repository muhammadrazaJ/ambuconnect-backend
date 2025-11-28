package com.example.ambuconnect_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripEndResponse {
    private Long tripId;
    private Long bookingId;
    private Long ambulanceId;
    private Long driverId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal fare;
    private String status;
    private Long paymentId;
    private String paymentStatus;
}
