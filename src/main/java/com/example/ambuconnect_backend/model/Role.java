package com.example.ambuconnect_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // PATIENT, DRIVER, ADMIN
    @Column(nullable = false, unique = true)
    private String name;
}