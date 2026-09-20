package com.hospital.his.pharmacy.entity;

import com.hospital.his.catalogs.entity.Branch;
import com.hospital.his.prescriptions.entity.Prescription;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "prescription_decline_events",
        indexes = {
                @Index(
                        name = "idx_prescription_decline_prescription",
                        columnList = "prescription_id"
                ),
                @Index(
                        name = "idx_prescription_decline_branch",
                        columnList = "branch_id"
                ),
                @Index(
                        name = "idx_prescription_decline_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionDeclineEvent {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    /**
     * Receta que el paciente decidió no adquirir
     * en esta visita a Farmacia.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "prescription_id",
            nullable = false
    )
    private Prescription prescription;

    /**
     * Sucursal donde fue registrada la decisión.
     *
     * Se obtiene del usuario autenticado.
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
     * Motivo indicado por el personal de Farmacia.
     */
    @Column(
            nullable = false,
            length = 500
    )
    private String reason;

    /**
     * Usuario de Farmacia obtenido desde el JWT.
     */
    @Column(
            name = "pharmacy_username",
            nullable = false,
            length = 100
    )
    private String pharmacyUsername;

    /**
     * Fecha y hora del registro.
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    /**
     * Control de concurrencia optimista.
     */
    @Version
    @Column(nullable = false)
    private Long version;
}