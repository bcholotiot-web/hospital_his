package com.hospital.his.appointments.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "appointment_reminders",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_appointment_reminder_appointment",
                        columnNames = "appointment_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_appointment_reminder_status_send_at",
                        columnList = "status, send_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentReminder {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    /**
     * Cita para la cual se enviará el recordatorio.
     *
     * Solo existe un recordatorio por cita.
     */
    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "appointment_id",
            nullable = false,
            unique = true
    )
    private Appointment appointment;

    /**
     * Fecha programada para enviar el recordatorio.
     *
     * Propuesta:
     * 24 horas antes de la cita.
     */
    @Column(
            name = "send_at",
            nullable = false
    )
    private LocalDateTime sendAt;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private AppointmentReminderStatus status;

    @Column(
            name = "attempt_count",
            nullable = false
    )
    private Integer attemptCount;

    @Column(
            name = "sent_at"
    )
    private LocalDateTime sentAt;

    @Column(
            name = "last_error",
            length = 1000
    )
    private String lastError;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;
}