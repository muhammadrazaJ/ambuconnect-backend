package com.example.ambuconnect_backend.repository;

import com.example.ambuconnect_backend.model.BookingRequest;
import com.example.ambuconnect_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRequestRepository extends JpaRepository<BookingRequest, Long> {
    List<BookingRequest> findByUser(User user);
}
