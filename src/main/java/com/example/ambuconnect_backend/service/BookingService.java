package com.example.ambuconnect_backend.service;

import com.example.ambuconnect_backend.dto.BookingRequestDTO;
import com.example.ambuconnect_backend.dto.BookingResponse;
import com.example.ambuconnect_backend.model.BookingRequest;
import com.example.ambuconnect_backend.model.Location;
import com.example.ambuconnect_backend.model.User;
import com.example.ambuconnect_backend.repository.BookingRequestRepository;
import com.example.ambuconnect_backend.repository.LocationRepository;
import com.example.ambuconnect_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRequestRepository bookingRequestRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;

    @Transactional
    public BookingResponse createBooking(BookingRequestDTO request) {
        // 1. Authenticate User - Extract email from JWT
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // Verify user has PATIENT role
        if (user.getRole() == null || !user.getRole().getName().equals("PATIENT")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Unauthorized user. Patient role required."
            );
        }

        // 2. Validate Locations
        Location pickupLocation = locationRepository.findById(request.getPickup_location_id())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Location not found"
                ));

        Location dropLocation = locationRepository.findById(request.getDrop_location_id())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Location not found"
                ));

        // 3. Create Booking Request
        BookingRequest bookingRequest = BookingRequest.builder()
                .user(user)
                .pickupLocation(pickupLocation)
                .dropLocation(dropLocation)
                .driverProfile(null)
                .status("pending")
                .build();

        BookingRequest savedBooking = bookingRequestRepository.save(bookingRequest);

        // 4. Return Response
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

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings() {

        // 1. Extract user email
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // Verify patient role
        if (user.getRole() == null || !user.getRole().getName().equals("PATIENT")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Unauthorized user. Patient role required."
            );
        }

        // 2. Fetch all user's bookings
        List<BookingRequest> bookings = bookingRequestRepository.findByUser(user);

        // 3. Map to BookingResponse (same structure as createBooking)
        return bookings.stream()
                .map(booking -> BookingResponse.builder()
                        .id(booking.getId())
                        .user_id(booking.getUser().getId())
                        .pickup_location_id(booking.getPickupLocation().getId())
                        .drop_location_id(booking.getDropLocation().getId())
                        .pickup_location(booking.getPickupLocation().getAddress())
                        .drop_location(booking.getDropLocation().getAddress())
                        .status(booking.getStatus())
                        .requested_at(booking.getRequestedAt())
                        .updated_at(booking.getUpdatedAt())
                        .build()
                )
                .toList();
    }


    @Transactional(readOnly = true)
    public List<BookingResponse> getGlobalPendingRequests() {

        // 1. Authenticate User - Extract email from JWT
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

        // 2. Fetch all pending bookings not assigned to any driver
        List<BookingRequest> pendingBookings = bookingRequestRepository
                .findByStatusAndDriverProfileIsNull("pending");

        // 3. Map to BookingResponse
        return pendingBookings.stream()
                .map(booking -> BookingResponse.builder()
                        .id(booking.getId())
                        .user_id(booking.getUser().getId())
                        .pickup_location_id(booking.getPickupLocation().getId())
                        .drop_location_id(booking.getDropLocation().getId())
                        .status(booking.getStatus())
                        .requested_at(booking.getRequestedAt())
                        .updated_at(booking.getUpdatedAt())
                        .build()
                )
                .toList();
    }
}
