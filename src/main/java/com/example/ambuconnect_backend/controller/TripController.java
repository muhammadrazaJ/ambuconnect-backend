package com.example.ambuconnect_backend.controller;

import com.example.ambuconnect_backend.dto.TripEndResponse;
import com.example.ambuconnect_backend.dto.TripStartResponse;
import com.example.ambuconnect_backend.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
@CrossOrigin
public class TripController {

    private final TripService tripService;

    @PostMapping("/{booking_id}/start")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<TripStartResponse> startTrip(@PathVariable("booking_id") Long bookingId) {
        TripStartResponse response = tripService.startTrip(bookingId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/end")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<TripEndResponse> endTrip(@PathVariable("id") Long tripId) {
        TripEndResponse response = tripService.endTrip(tripId);
        return ResponseEntity.ok(response);
    }
}
