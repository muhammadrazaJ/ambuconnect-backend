package com.example.ambuconnect_backend.service;

import com.example.ambuconnect_backend.dto.*;
import com.example.ambuconnect_backend.dto.TripDetailsResponse;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final UserRepository userRepository;
    private final com.example.ambuconnect_backend.repository.LocationRepository locationRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final TripRepository tripRepository;
    private final PaymentRepository paymentRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final AmbulanceTypeRepository ambulanceTypeRepository;

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

    public List<TripDetailsResponse> getPatientTripHistory() {
        // 1. Authenticate User - Extract email from JWT
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // 2. Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, 
                        "Patient profile not found"
                ));

        // 3. Verify user has PATIENT role
        if (user.getRole() == null || !user.getRole().getName().equalsIgnoreCase("PATIENT")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, 
                    "Access denied"
            );
        }

        // 4. Find all trips for this patient
        List<Trip> trips = tripRepository.findByBookingRequestUser(user);
        
        // 5. Map each trip to TripDetailsResponse
        return trips.stream()
                .map(this::mapToTripDetailsResponse)
                .collect(Collectors.toList());
    }

    private TripDetailsResponse mapToTripDetailsResponse(Trip trip) {
        BookingRequest bookingRequest = trip.getBookingRequest();
        
        // Get patient (user) info
        User patient = bookingRequest.getUser();
        TripDetailsResponse.UserInfo userInfo = TripDetailsResponse.UserInfo.builder()
                .name(patient.getName())
                .phone(patient.getPhone())
                .build();

        // Get driver info
        TripDetailsResponse.DriverInfo driverInfo = null;
        if (bookingRequest.getDriverProfile() != null) {
            DriverProfile driverProfile = bookingRequest.getDriverProfile();
            User driverUser = userRepository.findById(driverProfile.getUserId())
                    .orElse(null);

            if (driverUser != null) {
                // Get ambulance info
                Ambulance ambulance = ambulanceRepository.findById(trip.getAmbulanceId())
                        .orElse(null);

                String ambulanceTypeName = "Unknown";
                if (ambulance != null && ambulance.getAmbulanceType() != null) {
                    ambulanceTypeName = ambulance.getAmbulanceType().getType();
                }

                String ambulanceNumber = ambulance != null ? ambulance.getVehicleNumber() : "N/A";

                driverInfo = TripDetailsResponse.DriverInfo.builder()
                        .driverName(driverUser.getName())
                        .driverPhone(driverUser.getPhone())
                        .ambulanceNumber(ambulanceNumber)
                        .ambulanceType(ambulanceTypeName)
                        .build();
            }
        }

        // Get location info
        Location pickupLocation = bookingRequest.getPickupLocation();
        TripDetailsResponse.LocationInfo pickupLocationInfo = TripDetailsResponse.LocationInfo.builder()
                .address(pickupLocation.getAddress())
                .lat(pickupLocation.getLatitude())
                .lng(pickupLocation.getLongitude())
                .build();

        Location dropLocation = bookingRequest.getDropLocation();
        TripDetailsResponse.LocationInfo dropLocationInfo = TripDetailsResponse.LocationInfo.builder()
                .address(dropLocation.getAddress())
                .lat(dropLocation.getLatitude())
                .lng(dropLocation.getLongitude())
                .build();

        // Get payment info
        TripDetailsResponse.PaymentInfo paymentInfo = null;
        Payment payment = paymentRepository.findByTripId(trip.getId()).orElse(null);
        if (payment != null) {
            paymentInfo = TripDetailsResponse.PaymentInfo.builder()
                    .amount(payment.getAmount())
                    .method(payment.getMethod())
                    .status(payment.getStatus())
                    .build();
        }

        // Build and return response
        return TripDetailsResponse.builder()
                .tripId(trip.getId())
                .startTime(trip.getStartTime())
                .endTime(trip.getEndTime())
                .fare(trip.getFare())
                .status(trip.getStatus())
                .pickupLocation(pickupLocationInfo)
                .dropLocation(dropLocationInfo)
                .user(userInfo)
                .driver(driverInfo)
                .payment(paymentInfo)
                .build();
    }
}
