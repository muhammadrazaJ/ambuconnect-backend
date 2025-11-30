package com.example.ambuconnect_backend.controller;

import com.example.ambuconnect_backend.dto.BookingResponse;
import com.example.ambuconnect_backend.dto.PatientProfileResponse;
import com.example.ambuconnect_backend.dto.UpdatePatientProfileRequest;
import com.example.ambuconnect_backend.dto.TripDetailsResponse;
import com.example.ambuconnect_backend.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
@CrossOrigin
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientProfileResponse> getPatientProfile() {
        PatientProfileResponse profile = patientService.getPatientProfile();
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientProfileResponse> updatePatientProfile(
            @Valid @RequestBody UpdatePatientProfileRequest request) {
        PatientProfileResponse updatedProfile = patientService.updatePatientProfile(request);
        return ResponseEntity.ok(updatedProfile);
    }

    @PutMapping("/bookings/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long id) {
        BookingResponse response = patientService.cancelBooking(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trips")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<TripDetailsResponse>> getPatientTripHistory() {
        List<TripDetailsResponse> trips = patientService.getPatientTripHistory();
        return ResponseEntity.ok(trips);
    }
}
