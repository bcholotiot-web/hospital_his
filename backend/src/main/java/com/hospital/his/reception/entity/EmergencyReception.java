package com.hospital.his.reception.entity;

import com.hospital.his.catalogs.entity.Branch;
import com.hospital.his.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_receptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyReception {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Puede ser null cuando la persona todavía
     * no está registrada en el sistema.
     */
    @ManyToOne
    @JoinColumn(name = "patient_user_id")
    private User patient;

    @Column(
            name = "patient_name",
            nullable = false,
            length = 100
    )
    private String patientName;

    @Column(
            name = "patient_dpi",
            nullable = false,
            length = 13
    )
    private String patientDpi;

    @ManyToOne
    @JoinColumn(
            name = "branch_id",
            nullable = false
    )
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private EmergencyReceptionStatus status;

    @Column(
            name = "arrival_time",
            nullable = false
    )
    private LocalDateTime arrivalTime;

    @Column(
            name = "receptionist_username",
            nullable = false,
            length = 50
    )
    private String receptionistUsername;

    @Column(
            name = "emergency_note",
            length = 500
    )
    private String emergencyNote;

    @Column(nullable = false)
    private Boolean active;

    @Version
    @Column(nullable = false)
    private Long version;
}