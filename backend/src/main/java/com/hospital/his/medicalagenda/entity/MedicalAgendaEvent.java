package com.hospital.his.medicalagenda.entity;

import com.hospital.his.users.entity.User;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Evento personal registrado por un médico
 * dentro de su agenda.
 *
 * Los eventos son independientes de las citas,
 * pero pueden bloquear horarios disponibles
 * para nuevas citas y seguimientos.
 */
@Entity
@Table(
        name = "medical_agenda_events",
        indexes = {
                @Index(
                        name =
                                "idx_medical_agenda_event_doctor",
                        columnList =
                                "doctor_user_id"
                ),
                @Index(
                        name =
                                "idx_medical_agenda_event_start",
                        columnList =
                                "start_date_time"
                ),
                @Index(
                        name =
                                "idx_medical_agenda_event_end",
                        columnList =
                                "end_date_time"
                ),
                @Index(
                        name =
                                "idx_medical_agenda_event_doctor_range",
                        columnList =
                                "doctor_user_id, start_date_time, end_date_time"
                ),
                @Index(
                        name =
                                "idx_medical_agenda_event_active",
                        columnList =
                                "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalAgendaEvent {

    @Id
    @GeneratedValue(
            strategy =
                    GenerationType.IDENTITY
    )
    private Long id;

    /**
     * Médico propietario del evento.
     *
     * El usuario se obtiene desde la sesión.
     * El frontend no podrá asignar un evento
     * a otro médico.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "doctor_user_id",
            nullable = false
    )
    private User doctor;

    /**
     * Título visible en el calendario.
     */
    @Column(
            nullable = false,
            length = 150
    )
    private String title;

    /**
     * Descripción opcional.
     *
     * CU-16 limita el contenido a
     * 500 caracteres.
     */
    @Column(
            length = 500
    )
    private String description;

    /**
     * Inicio del evento.
     */
    @Column(
            name = "start_date_time",
            nullable = false
    )
    private LocalDateTime startDateTime;

    /**
     * Fin del evento.
     *
     * Debe ser posterior al inicio.
     */
    @Column(
            name = "end_date_time",
            nullable = false
    )
    private LocalDateTime endDateTime;

    /**
     * Indica si el evento ocupa días completos.
     *
     * Cuando sea true, el servicio normalizará:
     *
     * inicio → 00:00 del primer día
     * fin    → 00:00 del día siguiente al último
     */
    @Builder.Default
    @Column(
            name = "all_day",
            nullable = false
    )
    private Boolean allDay = false;

    /**
     * Tipo funcional del evento.
     */
    @Enumerated(
            EnumType.STRING
    )
    @Column(
            name = "event_type",
            nullable = false,
            length = 30
    )
    private MedicalAgendaEventType eventType;

    /**
     * Color utilizado en el calendario.
     *
     * El usuario no puede configurarlo.
     * El servicio siempre asignará #8B5CF6.
     */
    @Column(
            nullable = false,
            length = 7
    )
    private String color;

    /**
     * Estado lógico.
     *
     * Un evento eliminado permanece
     * disponible para auditoría.
     */
    @Builder.Default
    @Column(
            nullable = false
    )
    private Boolean active = true;

    /**
     * Fecha de creación.
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    /**
     * Usuario responsable de la creación.
     */
    @Column(
            name = "created_by",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String createdBy;

    /**
     * Fecha de última modificación.
     */
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    /**
     * Usuario responsable de la última
     * modificación.
     */
    @Column(
            name = "updated_by",
            nullable = false,
            length = 100
    )
    private String updatedBy;

    /**
     * Control de concurrencia optimista.
     */
    @Version
    @Column(
            nullable = false
    )
    private Long version;
}