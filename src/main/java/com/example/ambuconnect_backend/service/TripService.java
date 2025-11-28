package com.example.ambuconnect_backend.service;

import com.example.ambuconnect_backend.dto.TripStartResponse;
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
public class TripService {

    private final TripRepository tripRepository;
    private final TripStatusHistoryRepository tripStatusHistoryRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final AmbulanceAvailabilityRepository ambulanceAvailabilityRepository;

    @Transactional
    public TripStartResponse startTrip(Long bookingId) {
        // 1. Authenticate User - Extract email from JWT
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // 2. Verify user has DRIVER role
        if (user.getRole() == null || !user.getRole().getName().equals("DRIVER")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Unauthorized user. Driver role required."
            );
        }

        // 3. Get DriverProfile for authenticated user
        DriverProfile driverProfile = driverProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Driver profile not found"
                ));

        // 4. Validate booking exists
        BookingRequest bookingRequest = bookingRequestRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        // 5. Ensure driver is assigned to this booking
        if (bookingRequest.getDriverProfile() == null ||
                !bookingRequest.getDriverProfile().getId().equals(driverProfile.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Driver is not assigned to this booking"
            );
        }

        // 6. Ensure booking status is "accepted"
        if (!"accepted".equals(bookingRequest.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking must be in 'accepted' status to start trip. Current status: " + bookingRequest.getStatus()
            );
        }

        // 7. Check if trip already exists for this booking
        if (tripRepository.findByBookingRequestId(bookingId).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trip already exists for this booking"
            );
        }

        // 8. Get ambulance for this driver
        List<Ambulance> ambulances = ambulanceRepository.findAllByDriverProfileId(driverProfile.getId());
        if (ambulances.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No ambulance found for this driver"
            );
        }
        // Use the first ambulance (assuming one driver has one ambulance)
        Ambulance ambulance = ambulances.get(0);

        // 9. Check ambulance availability
        AmbulanceAvailability ambulanceAvailability = ambulanceAvailabilityRepository
                .findByAmbulanceId(ambulance.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ambulance availability record not found"
                ));

//        if (!ambulanceAvailability.getIsAvailable()) {
//            throw new ResponseStatusException(
//                    HttpStatus.BAD_REQUEST,
//                    "Ambulance is not available"
//            );
//        }

        // 10. Create Trip record
        LocalDateTime now = LocalDateTime.now();
        Trip trip = Trip.builder()
                .bookingRequest(bookingRequest)
                .ambulanceId(ambulance.getId())
                .startTime(now)
                .endTime(null)
                .fare(null)
                .status("in_progress")
                .build();

        Trip savedTrip = tripRepository.save(trip);

        // 11. Update BOOKING_REQUESTS.status to "in_progress"
        bookingRequest.setStatus("in_progress");
        bookingRequestRepository.save(bookingRequest);

        // 12. Update AMBULANCE_AVAILABILITY.is_available to false
        ambulanceAvailability.setIsAvailable(false);
        ambulanceAvailabilityRepository.save(ambulanceAvailability);

        // 13. Insert into TRIP_STATUS_HISTORY
        TripStatusHistory tripStatusHistory = TripStatusHistory.builder()
                .trip(savedTrip)
                .status("in_progress")
                .build();
        tripStatusHistoryRepository.save(tripStatusHistory);

        // 14. Return TripStartResponse
        return TripStartResponse.builder()
                .tripId(savedTrip.getId())
                .bookingId(bookingRequest.getId())
                .ambulanceId(ambulance.getId())
                .driverId(driverProfile.getId())
                .startTime(now)
                .status("in_progress")
                .build();
    }
}
