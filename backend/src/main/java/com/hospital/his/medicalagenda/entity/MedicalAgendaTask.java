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
 * Tarea o recordatorio personal de un médico.
 *
 * Las tareas aparecen en el panel lateral
 * TaskPanel de la agenda médica.
 */
@Entity
@Table(
        name = "medical_agenda_tasks",
        indexes = {
                @Index(
                        name =
                                "idx_medical_agenda_task_doctor",
                        columnList =
                                "doctor_user_id"
                ),
                @Index(
                        name =
                                "idx_medical_agenda_task_due",
                        columnList =
                                "due_date_time"
                ),
                @Index(
                        name =
                                "idx_medical_agenda_task_completed",
                        columnList =
                                "completed"
                ),
                @Index(
                        name =
                                "idx_medical_agenda_task_doctor_due",
                        columnList =
                                "doctor_user_id, due_date_time"
                ),
                @Index(
                        name =
                                "idx_medical_agenda_task_active",
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
public class MedicalAgendaTask {

    @Id
    @GeneratedValue(
            strategy =
                    GenerationType.IDENTITY
    )
    private Long id;

    /**
     * Médico propietario de la tarea.
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
     * Título breve de la tarea.
     */
    @Column(
            nullable = false,
            length = 150
    )
    private String title;

    /**
     * Descripción opcional.
     *
     * CU-16 permite máximo
     * 1000 caracteres.
     */
    @Column(
            length = 1000
    )
    private String description;

    /**
     * Prioridad de la tarea.
     */
    @Enumerated(
            EnumType.STRING
    )
    @Column(
            nullable = false,
            length = 20
    )
    private MedicalAgendaTaskPriority priority;

    /**
     * Fecha y hora límite.
     */
    @Column(
            name = "due_date_time",
            nullable = false
    )
    private LocalDateTime dueDateTime;

    /**
     * Indica si la tarea está completada.
     */
    @Builder.Default
    @Column(
            nullable = false
    )
    private Boolean completed = false;

    /**
     * Fecha en la que se completó.
     *
     * Al reabrir una tarea regresará a null.
     */
    @Column(
            name = "completed_at"
    )
    private LocalDateTime completedAt;

    /**
     * Estado lógico de la tarea.
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
     * Usuario responsable de crear la tarea.
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