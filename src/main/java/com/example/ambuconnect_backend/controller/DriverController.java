package com.example.ambuconnect_backend.controller;

import com.example.ambuconnect_backend.dto.*;
import com.example.ambuconnect_backend.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
@CrossOrigin
public class DriverController {

    private final DriverService driverService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverRegisterResponse> registerDriver(
            @Valid @RequestBody DriverRegisterRequest request) {
        DriverRegisterResponse response = driverService.registerDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverProfileResponse> getDriverProfile() {
        DriverProfileResponse response = driverService.getCurrentDriverProfile();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/status")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse> updateDriverStatus(
            @Valid @RequestBody UpdateDriverStatusRequest request) {
        ApiResponse response = driverService.updateDriverStatus(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/bookings/{id}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<BookingResponse> acceptBooking(@PathVariable Long id) {
        BookingResponse response = driverService.acceptBooking(id);
        return ResponseEntity.ok(response);
    }
}
