package com.hospital.his.appointments.entity;

import com.hospital.his.catalogs.entity.Branch;
import com.hospital.his.catalogs.entity.Specialty;
import com.hospital.his.users.entity.User;
import com.hospital.his.medicalconsultation.entity.MedicalConsultation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

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

    /*
    Version nos servira para
    Recepcionista A abre la cita
    Recepcionista B registra la llegada
    Recepcionista A intenta registrar también
    Hibernate detecta que la versión cambió
    Se evita sobrescribir el cambio
    * */
    @Version
    @Column(nullable = false)
    private Long version;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private AppointmentPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentOrigin origin;

    /**
     * Indica si la cita fue creada como seguimiento
     * de una consulta médica previa.
     *
     * false:
     * cita regular creada por el paciente.
     *
     * true:
     * cita posterior programada por el médico.
     */
    @Column(name = "follow_up", nullable = false)
    private Boolean followUp;

    /**
     * Tipo funcional del seguimiento.
     *
     * Solo debe tener valor cuando followUp es true.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "follow_up_type", length = 50)
    private FollowUpType followUpType;

    /**
     * Consulta médica que originó la cita
     * de seguimiento.
     *
     * Para citas regulares este campo será null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_consultation_id")
    private MedicalConsultation parentConsultation;

    /**
     * Llave de idempotencia utilizada únicamente
     * durante la creación de citas de seguimiento.
     *
     * Para citas regulares puede permanecer null.
     */
    @Column(name = "idempotency_key", unique = true)
    private UUID idempotencyKey;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Column(nullable = false)
    private Boolean active;
}