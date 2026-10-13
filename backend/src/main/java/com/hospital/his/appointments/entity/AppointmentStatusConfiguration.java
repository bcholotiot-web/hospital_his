package com.hospital.his.appointments.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * CU-14:
 * Configuración visual y administrativa de un
 * estado técnico de cita.
 *
 * Esta entidad no reemplaza AppointmentStatus.
 * Solamente permite configurar:
 *
 * - Nombre visible.
 * - Descripción.
 * - Color.
 * - Orden.
 * - Visibilidad.
 * - Estado lógico de la configuración.
 */
@Entity
@Table(
        name = "appointment_status_configurations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_appointment_status_config_code",
                        columnNames = "code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_appointment_status_config_code",
                        columnList = "code"
                ),
                @Index(
                        name = "idx_appointment_status_config_active",
                        columnList = "active"
                ),
                @Index(
                        name = "idx_appointment_status_config_order",
                        columnList = "display_order"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentStatusConfiguration {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    /**
     * Código técnico existente en AppointmentStatus.
     *
     * Ejemplo:
     * PENDIENTE_DE_PAGO
     */
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            unique = true,
            length = 40
    )
    private AppointmentStatus code;

    /**
     * Texto visible para el usuario.
     *
     * Ejemplo:
     * Pendiente de pago
     */
    @Column(
            name = "display_name",
            nullable = false,
            length = 100
    )
    private String displayName;

    /**
     * Explicación funcional del estado.
     */
    @Column(
            nullable = false,
            length = 500
    )
    private String description;

    /**
     * Posición visual del estado en listados.
     */
    @Column(
            name = "display_order",
            nullable = false
    )
    private Integer displayOrder;

    /**
     * Color hexadecimal utilizado por React.
     *
     * Ejemplo:
     * #F59E0B
     */
    @Column(
            nullable = false,
            length = 7
    )
    private String color;

    /**
     * Indica si el estado puede mostrarse
     * al paciente.
     */
    @Column(
            name = "patient_visible",
            nullable = false
    )
    private Boolean patientVisible;

    /**
     * Indica si el estado puede mostrarse
     * al personal hospitalario.
     */
    @Column(
            name = "staff_visible",
            nullable = false
    )
    private Boolean staffVisible;

    /**
     * Eliminación lógica de la configuración.
     *
     * No cambia el estado técnico de las citas.
     */
    @Column(
            nullable = false
    )
    private Boolean active;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "created_by",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String createdBy;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

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