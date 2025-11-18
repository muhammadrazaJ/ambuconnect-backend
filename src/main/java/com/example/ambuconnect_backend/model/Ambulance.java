package com.example.ambuconnect_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "ambulances", uniqueConstraints = {
    @UniqueConstraint(columnNames = "vehicle_number")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Ambulance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_profile_id", nullable = false)
    private Long driverProfileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_profile_id", insertable = false, updatable = false)
    private DriverProfile driverProfile;

    @Column(name = "ambulance_type_id", nullable = false)
    private Integer ambulanceTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambulance_type_id", insertable = false, updatable = false)
    private AmbulanceType ambulanceType;

    @Column(name = "vehicle_number", nullable = false, unique = true)
    private String vehicleNumber;

    @Column(nullable = false)
    private String model;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
