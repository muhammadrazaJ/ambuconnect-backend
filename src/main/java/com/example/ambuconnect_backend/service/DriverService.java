package com.example.ambuconnect_backend.service;

import com.example.ambuconnect_backend.dto.*;
import com.example.ambuconnect_backend.model.*;
import com.example.ambuconnect_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverProfileRepository driverProfileRepository;
    private final UserRepository userRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final AmbulanceAvailabilityRepository ambulanceAvailabilityRepository;

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

    public ApiResponse updateDriverStatus(UpdateDriverStatusRequest request) {
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
                    "Unauthorized user. Driver role required."
            );
        }

        // Find driver profile by userId
        DriverProfile driverProfile = driverProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Driver profile not found"
                ));

        // Validate and convert status string to enum
        DriverStatus status;
        try {
            status = DriverStatus.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid status. Status must be either 'Online' or 'Offline'"
            );
        }

        // Update status
        driverProfile.setStatus(status);
        driverProfileRepository.save(driverProfile);

        return new ApiResponse(true, "Driver status updated successfully");
    }

    @Transactional
    public BookingResponse acceptBooking(Long bookingId) {
        // 1. Authenticate Driver - Extract email from JWT
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // Verify user has DRIVER role
        if (user.getRole() == null || !user.getRole().getName().equals("DRIVER")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Unauthorized user. Driver role required."
            );
        }

        // 2. Get Driver Profile
        DriverProfile driverProfile = driverProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Driver profile not found"
                ));

        // 3. Find Booking
        BookingRequest booking = bookingRequestRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        // 4. Validate Booking Status
        if (!"pending".equals(booking.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking is not pending. Current status: " + booking.getStatus()
            );
        }

        // 5. Find Ambulance linked to this driver
        List<Ambulance> ambulances = ambulanceRepository.findAllByDriverProfileId(driverProfile.getId());

        if (ambulances.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No ambulance found for this driver"
            );
        }

        // Get the first ambulance (assuming one driver has one ambulance)
        // If multiple ambulances exist, you might want to find an available one
        Ambulance ambulance = ambulances.get(0);

        // 6. Find Ambulance Availability
        AmbulanceAvailability availability = ambulanceAvailabilityRepository.findByAmbulanceId(ambulance.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ambulance availability record not found"
                ));

        // 7. Check if Ambulance is Available
        if (!Boolean.TRUE.equals(availability.getIsAvailable())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ambulance is not available"
            );
        }

        // 8. Update Booking
        booking.setStatus("accepted");
        booking.setDriverProfile(driverProfile);
        booking.setUpdatedAt(LocalDateTime.now());
        BookingRequest savedBooking = bookingRequestRepository.save(booking);

        // 9. Update Ambulance Availability
        availability.setIsAvailable(false);
        ambulanceAvailabilityRepository.save(availability);

        // 10. Build and Return BookingResponse
        return BookingResponse.builder()
                .id(savedBooking.getId())
                .user_id(savedBooking.getUser().getId())
                .pickup_location_id(savedBooking.getPickupLocation().getId())
                .drop_location_id(savedBooking.getDropLocation().getId())
                .status(savedBooking.getStatus())
                .requested_at(savedBooking.getRequestedAt())
                .updated_at(savedBooking.getUpdatedAt())
                .build();
    }
}
