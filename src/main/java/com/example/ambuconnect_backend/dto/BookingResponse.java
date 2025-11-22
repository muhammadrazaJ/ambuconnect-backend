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
public class BookingResponse {
    private Long id;
    private Long user_id;
    private Long pickup_location_id;
    private Long drop_location_id;
    private String status;
    private LocalDateTime requested_at;
    private LocalDateTime updated_at;
}
