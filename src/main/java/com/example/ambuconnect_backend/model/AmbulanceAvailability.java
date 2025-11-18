package com.example.ambuconnect_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "ambulance_availability")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class AmbulanceAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ambulance_id", nullable = false, unique = true)
    private Long ambulanceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambulance_id", insertable = false, updatable = false)
    private Ambulance ambulance;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
