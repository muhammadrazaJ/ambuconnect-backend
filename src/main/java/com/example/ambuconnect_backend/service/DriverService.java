package com.example.ambuconnect_backend.service;

import com.example.ambuconnect_backend.dto.DriverProfileResponse;
import com.example.ambuconnect_backend.dto.DriverRegisterRequest;
import com.example.ambuconnect_backend.dto.DriverRegisterResponse;
import com.example.ambuconnect_backend.model.DriverProfile;
import com.example.ambuconnect_backend.model.User;
import com.example.ambuconnect_backend.repository.DriverProfileRepository;
import com.example.ambuconnect_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverProfileRepository driverProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public DriverRegisterResponse registerDriver(DriverRegisterRequest request) {
        // Validate user_id exists
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User with ID " + request.getUserId() + " not found"
                ));

        // Validate user doesn't already have a driver profile
        if (driverProfileRepository.existsByUserId(request.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User already has a driver profile"
            );
        }

        // Validate license_number is not empty (already validated by @NotBlank, but double-check)
        String licenseNumber = request.getLicenseNumber().trim();
        if (licenseNumber.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "License number cannot be empty"
            );
        }

        // Validate CNIC is exactly 13 digits (already validated by @Pattern, but double-check)
        String cnic = request.getCnic().trim();
        if (!cnic.matches("^\\d{13}$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "CNIC must be exactly 13 digits"
            );
        }

        // Check for duplicate CNIC
        if (driverProfileRepository.existsByCnic(cnic)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "CNIC already registered"
            );
        }

        // Check for duplicate license_number
        if (driverProfileRepository.existsByLicenseNumber(licenseNumber)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "License number already registered"
            );
        }

        // Create and save driver profile
        DriverProfile driverProfile = DriverProfile.builder()
                .userId(request.getUserId())
                .licenseNumber(licenseNumber)
                .cnic(cnic)
                .build();

        DriverProfile savedProfile = driverProfileRepository.save(driverProfile);

        // Build and return response
        return DriverRegisterResponse.builder()
                .success(true)
                .message("Driver profile registered successfully")
                .id(savedProfile.getId())
                .userId(savedProfile.getUserId())
                .licenseNumber(savedProfile.getLicenseNumber())
                .cnic(savedProfile.getCnic())
                .createdAt(savedProfile.getCreatedAt())
                .build();
    }

    public DriverProfileResponse getCurrentDriverProfile() {
        // Get current authenticated user's email from SecurityContext
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // Verify user has DRIVER role
        if (user.getRole() == null || !user.getRole().getName().equals("DRIVER")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied. Driver role required."
            );
        }

        // Find driver profile by userId
        DriverProfile driverProfile = driverProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Driver profile not found"
                ));


        return DriverProfileResponse.builder()
                .success(true)
                .message("Driver profile fetched successfully")
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .role(user.getRole().getName())
                .cnic(driverProfile.getCnic())
                .licenseNumber(driverProfile.getLicenseNumber())
                .build();
    }
}
