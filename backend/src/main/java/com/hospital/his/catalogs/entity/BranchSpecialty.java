package com.hospital.his.catalogs.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * CU-13:
 * Representa la relación entre una sede y una
 * especialidad disponible en dicha sede.
 *
 * Esta entidad implementa la regla RN-CU12-01:
 * una combinación sede-especialidad debe ser única.
 */
@Entity
@Table(
        name = "branch_specialties",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_branch_specialty",
                        columnNames = {
                                "branch_id",
                                "specialty_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_branch_specialty_branch",
                        columnList = "branch_id"
                ),
                @Index(
                        name = "idx_branch_specialty_specialty",
                        columnList = "specialty_id"
                ),
                @Index(
                        name = "idx_branch_specialty_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchSpecialty {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    /**
     * Sede en la que estará disponible
     * la especialidad.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "branch_id",
            nullable = false
    )
    private Branch branch;

    /**
     * Especialidad asignada a la sede.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "specialty_id",
            nullable = false
    )
    private Specialty specialty;

    /**
     * Estado lógico de la asignación.
     *
     * true:
     * la especialidad está disponible en la sede.
     *
     * false:
     * la relación permanece como histórico,
     * pero no aparece en el agendamiento.
     */
    @Column(
            nullable = false
    )
    private Boolean active;

    /**
     * Fecha en que se creó originalmente
     * la asignación.
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    /**
     * Usuario administrador que creó
     * originalmente la relación.
     */
    @Column(
            name = "created_by",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String createdBy;

    /**
     * Fecha de la última modificación.
     */
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    /**
     * Usuario que realizó la última modificación
     * o reactivación.
     */
    @Column(
            name = "updated_by",
            nullable = false,
            length = 100
    )
    private String updatedBy;

    /**
     * Control de concurrencia optimista.
     *
     * Evita que dos administradores sobrescriban
     * accidentalmente la misma asignación.
     */
    @Version
    @Column(
            nullable = false
    )
    private Long version;
}