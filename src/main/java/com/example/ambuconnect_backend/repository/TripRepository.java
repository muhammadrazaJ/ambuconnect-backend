package com.example.ambuconnect_backend.repository;

import com.example.ambuconnect_backend.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    Optional<Trip> findByBookingRequestId(Long bookingRequestId);
    List<Trip> findByStatus(String status);
}
