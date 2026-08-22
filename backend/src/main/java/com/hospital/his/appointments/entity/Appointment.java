package com.hospital.his.appointments.entity;

import com.hospital.his.catalogs.entity.Branch;
import com.hospital.his.catalogs.entity.Specialty;
import com.hospital.his.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Paciente que agenda la cita
    @ManyToOne
    @JoinColumn(name = "patient_user_id", nullable = false)
    private User patient;

    // Médico seleccionado
    @ManyToOne
    @JoinColumn(name = "doctor_user_id", nullable = false)
    private User doctor;

    //Secursal asociado a la cita
    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    //Especialidad solicitada para la cita
    @ManyToOne
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;

    @Column(nullable = false)
    private LocalDateTime appointmentDateTime;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AppointmentStatus status;

    @Column(name = "reservation_expires_at", nullable = false)
    private LocalDateTime reservationExpiresAt;

    @Column(nullable = false)
    private Boolean active;


}