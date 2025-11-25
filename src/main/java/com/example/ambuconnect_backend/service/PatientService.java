package com.example.ambuconnect_backend.service;

import com.example.ambuconnect_backend.dto.*;
import com.example.ambuconnect_backend.model.BookingRequest;
import com.example.ambuconnect_backend.model.Location;
import com.example.ambuconnect_backend.model.User;
import com.example.ambuconnect_backend.repository.BookingRequestRepository;
import com.example.ambuconnect_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final UserRepository userRepository;
    private final com.example.ambuconnect_backend.repository.LocationRepository locationRepository;
    private final BookingRequestRepository bookingRequestRepository;

    public PatientProfileResponse getPatientProfile() {
        // Get email from SecurityContext (set by JwtAuthFilter)
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, 
                        "Patient profile not found"
                ));

        // Verify user has PATIENT role
        if (user.getRole() == null || !user.getRole().getName().equalsIgnoreCase("PATIENT")) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, 
                    "Patient profile not found"
            );
        }

        // Return patient profile
        return new PatientProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().getName()
        );
    }

    public PatientProfileResponse updatePatientProfile(UpdatePatientProfileRequest request) {
        // Get email from SecurityContext (set by JwtAuthFilter)
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Patient profile not found"
                ));

        // Verify user has PATIENT role
        if (user.getRole() == null || !user.getRole().getName().equalsIgnoreCase("PATIENT")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied"
            );
        }

        // Validate name
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Name cannot be empty"
            );
        }

        // Validate phone format
        String phone = request.getPhone().trim();
        if (phone.length() < 10 || phone.length() > 15) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phone number must be between 10 and 15 characters"
            );
        }

        // Optional: More strict phone format validation
        // Remove common formatting characters for validation
        String phoneDigits = phone.replaceAll("[^0-9+]", "");
        if (phoneDigits.length() < 10) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phone number format is invalid"
            );
        }

        // Check phone uniqueness (but allow if it's the current user's phone)
        userRepository.findByPhone(phone).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(user.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Phone number already registered"
                );
            }
        });

        // Update only allowed fields
        user.setName(request.getName().trim());
        user.setPhone(phone);

        // Save updated user
        User updatedUser = userRepository.save(user);

        // Return updated patient profile
        return new PatientProfileResponse(
                updatedUser.getId(),
                updatedUser.getName(),
                updatedUser.getEmail(),
                updatedUser.getPhone(),
                updatedUser.getRole().getName()
        );
    }

    public SaveLocationResponse saveLocation(SaveLocationRequest request) {
        // Get email from SecurityContext (set by JwtAuthFilter)
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Patient profile not found"
                ));

        // Verify user has PATIENT role
        if (user.getRole() == null || !user.getRole().getName().equalsIgnoreCase("PATIENT")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied"
            );
        }

        // Validate address
        if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Address cannot be empty"
            );
        }

        // Check for existing location with same lat/long
        Optional<Location> existingLocation = locationRepository.findByLatitudeAndLongitude(
                request.getLatitude(),
                request.getLongitude()
        );

        if (existingLocation.isPresent()) {
            return new SaveLocationResponse(
                    existingLocation.get().getId(),
                    "Location saved successfully"
            );
        }

        // Create new location
        Location newLocation = Location.builder()
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        Location savedLocation = locationRepository.save(newLocation);

        return new SaveLocationResponse(
                savedLocation.getId(),
                "Location saved successfully"
        );
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        // 1. Authenticate User - Extract email from JWT
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // Verify user has PATIENT role
        if (user.getRole() == null || !user.getRole().getName().equalsIgnoreCase("PATIENT")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied"
            );
        }

        // 2. Find Booking
        BookingRequest booking = bookingRequestRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        // 3. Verify Booking Ownership
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to cancel this booking"
            );
        }

        // 4. Validate Booking Status - Can only cancel pending bookings
        if (!"pending".equals(booking.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending bookings can be cancelled. Current status: " + booking.getStatus()
            );
        }

        // 5. Update Booking Status to Cancelled
        booking.setStatus("cancelled");
        booking.setUpdatedAt(LocalDateTime.now());
        BookingRequest savedBooking = bookingRequestRepository.save(booking);

        // 6. Build and Return BookingResponse
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
