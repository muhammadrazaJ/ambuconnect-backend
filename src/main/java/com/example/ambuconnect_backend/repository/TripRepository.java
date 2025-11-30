package com.example.ambuconnect_backend.repository;

import com.example.ambuconnect_backend.model.Trip;
import com.example.ambuconnect_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    Optional<Trip> findByBookingRequestId(Long bookingRequestId);
    List<Trip> findByStatus(String status);
    
    @Query("SELECT t FROM Trip t WHERE t.bookingRequest.user = :user ORDER BY t.startTime DESC")
    List<Trip> findByBookingRequestUser(@Param("user") User user);
}
