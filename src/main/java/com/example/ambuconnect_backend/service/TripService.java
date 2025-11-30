package com.example.ambuconnect_backend.service;

import com.example.ambuconnect_backend.dto.TripEndResponse;
import com.example.ambuconnect_backend.dto.TripStartResponse;
import com.example.ambuconnect_backend.model.*;
import com.example.ambuconnect_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import com.example.ambuconnect_backend.repository.AmbulanceTypeRepository;
import com.example.ambuconnect_backend.dto.TripDetailsResponse;

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
    private final PaymentRepository paymentRepository;
    private final AmbulanceTypeRepository ambulanceTypeRepository;

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

    @Transactional
    public TripEndResponse endTrip(Long tripId) {
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

        // 4. Fetch trip by ID
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Trip not found"
                ));

        // 5. Validate that trip is in "in_progress" status
        if (!"in_progress".equals(trip.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trip must be in 'in_progress' status to end. Current status: " + trip.getStatus()
            );
        }

        // 6. Verify driver is assigned to this trip
        BookingRequest bookingRequest = trip.getBookingRequest();
        if (bookingRequest.getDriverProfile() == null ||
                !bookingRequest.getDriverProfile().getId().equals(driverProfile.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Driver is not assigned to this trip"
            );
        }

        // 7. Check if payment already exists for this trip
        if (paymentRepository.findByTripId(tripId).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment already exists for this trip"
            );
        }

        // 8. Set end time = now()
        LocalDateTime endTime = LocalDateTime.now();

        // 9. Calculate fare using duration
        // Formula: baseFare = 500 + (5 * tripDurationInMinutes)
        Duration duration = Duration.between(trip.getStartTime(), endTime);
        long tripDurationInMinutes = duration.toMinutes();
        BigDecimal fare = BigDecimal.valueOf(500 + (5 * tripDurationInMinutes));

        // 10. Update trip record: end_time, fare, status = completed
        trip.setEndTime(endTime);
        trip.setFare(fare);
        trip.setStatus("completed");
        Trip savedTrip = tripRepository.save(trip);

        // 11. Insert into TRIP_STATUS_HISTORY (trip_id, status = completed, changed_at)
        TripStatusHistory tripStatusHistory = TripStatusHistory.builder()
                .trip(savedTrip)
                .status("completed")
                .build();
        tripStatusHistoryRepository.save(tripStatusHistory);

        // 12. Update AMBULANCE_AVAILABILITY: is_available = true
        AmbulanceAvailability ambulanceAvailability = ambulanceAvailabilityRepository
                .findByAmbulanceId(trip.getAmbulanceId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ambulance availability record not found"
                ));
        ambulanceAvailability.setIsAvailable(true);
        ambulanceAvailabilityRepository.save(ambulanceAvailability);

        // 13. Update BOOKING_REQUESTS: status = completed
        bookingRequest.setStatus("completed");
        bookingRequestRepository.save(bookingRequest);

        // 14. Insert into PAYMENTS: amount = fare, method = cash, status = pending
        Payment payment = Payment.builder()
                .trip(savedTrip)
                .amount(fare)
                .method("cash")
                .status("pending")
                .paidAt(null)
                .build();
        Payment savedPayment = paymentRepository.save(payment);

        // 15. Return TripEndResponse
        return TripEndResponse.builder()
                .tripId(savedTrip.getId())
                .bookingId(bookingRequest.getId())
                .ambulanceId(trip.getAmbulanceId())
                .driverId(driverProfile.getId())
                .startTime(trip.getStartTime())
                .endTime(endTime)
                .fare(fare)
                .status("completed")
                .paymentId(savedPayment.getId())
                .paymentStatus(savedPayment.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public TripDetailsResponse getTripDetails(Long tripId) {
        // 1. Authenticate User - Extract email from JWT
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // 2. Fetch trip by ID
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Trip not found"
                ));

        // 3. Get booking request
        BookingRequest bookingRequest = trip.getBookingRequest();
        if (bookingRequest == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Booking request not found for this trip"
            );
        }

        // 4. Verify user has access: either patient (user who made booking) OR driver
        boolean isPatient = bookingRequest.getUser() != null && 
                           bookingRequest.getUser().getId().equals(authenticatedUser.getId());
        
        boolean isDriver = false;
        if (bookingRequest.getDriverProfile() != null) {
            DriverProfile driverProfile = bookingRequest.getDriverProfile();
            isDriver = driverProfile.getUserId().equals(authenticatedUser.getId());
        }

        if (!isPatient && !isDriver) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Unauthorized access. Trip does not belong to this user."
            );
        }

        // 5. Get patient (user) info
        User patient = bookingRequest.getUser();
        TripDetailsResponse.UserInfo userInfo = TripDetailsResponse.UserInfo.builder()
                .name(patient.getName())
                .phone(patient.getPhone())
                .build();

        // 6. Get driver info
        TripDetailsResponse.DriverInfo driverInfo = null;
        if (bookingRequest.getDriverProfile() != null) {
            DriverProfile driverProfile = bookingRequest.getDriverProfile();
            User driverUser = userRepository.findById(driverProfile.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Driver user not found"
                    ));

            // Get ambulance info
            Ambulance ambulance = ambulanceRepository.findById(trip.getAmbulanceId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Ambulance not found"
                    ));

            // Get ambulance type
            String ambulanceTypeName = "Unknown";
            if (ambulance.getAmbulanceType() != null) {
                ambulanceTypeName = ambulance.getAmbulanceType().getType();
            }
//            else {
//                // Fetch explicitly if lazy loading didn't work
//                ambulanceTypeRepository.findById(ambulance.getAmbulanceTypeId())
//                        .ifPresent(type -> ambulanceTypeName = type.getType());
//            }

            driverInfo = TripDetailsResponse.DriverInfo.builder()
                    .driverName(driverUser.getName())
                    .driverPhone(driverUser.getPhone())
                    .ambulanceNumber(ambulance.getVehicleNumber())
                    .ambulanceType(ambulanceTypeName)
                    .build();
        }

        // 7. Get location info
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

        // 8. Get payment info
        TripDetailsResponse.PaymentInfo paymentInfo = null;
        Payment payment = paymentRepository.findByTripId(tripId).orElse(null);
        if (payment != null) {
            paymentInfo = TripDetailsResponse.PaymentInfo.builder()
                    .amount(payment.getAmount())
                    .method(payment.getMethod())
                    .status(payment.getStatus())
                    .build();
        }

        // 9. Build and return response
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
