package com.example.ambuconnect_backend.controller;

import com.example.ambuconnect_backend.dto.BookingRequestDTO;
import com.example.ambuconnect_backend.dto.BookingResponse;
import com.example.ambuconnect_backend.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequestDTO request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<BookingResponse>> getUserBookings() {
        List<BookingResponse> response = bookingService.getUserBookings();
        return ResponseEntity.ok(response);
    }
}
