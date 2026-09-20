package com.hospital.his.prescriptions.entity;

import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.medicalconsultation.entity.MedicalConsultation;
import com.hospital.his.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "prescriptions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_prescription_number",
                        columnNames = "prescription_number"
                )
        },
        indexes = {
                @Index(
                        name = "idx_prescription_consultation",
                        columnList = "medical_consultation_id"
                ),
                @Index(
                        name = "idx_prescription_patient",
                        columnList = "patient_user_id"
                ),
                @Index(
                        name = "idx_prescription_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_prescription_issued_at",
                        columnList = "issued_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    /**
     * Número visible y único de la receta.
     *
     * Ejemplo:
     * REC-A1B2C3D4E5F6
     */
    @Column(
            name = "prescription_number",
            nullable = false,
            length = 40
    )
    private String prescriptionNumber;

    /**
     * Consulta médica desde la cual se generó
     * la receta.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "medical_consultation_id",
            nullable = false
    )
    private MedicalConsultation medicalConsultation;

    /**
     * Cita asociada con la consulta.
     *
     * Se conserva para facilitar búsquedas y trazabilidad.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "appointment_id",
            nullable = false
    )
    private Appointment appointment;

    /**
     * Paciente para quien fue emitida la receta.
     *
     * Este dato se obtiene desde la cita y nunca
     * desde el request.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "patient_user_id",
            nullable = false
    )
    private User patient;

    /**
     * Médico que emitió la receta.
     *
     * Se obtiene desde el usuario autenticado.
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

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private PrescriptionStatus status;

    /**
     * Observaciones generales opcionales.
     */
    @Column(
            length = 1000
    )
    private String notes;

    /**
     * Fecha exacta de emisión.
     *
     * Es asignada exclusivamente por el backend.
     */
    @Column(
            name = "issued_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime issuedAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    /**
     * Fecha en la cual todos los medicamentos
     * fueron despachados.
     */
    @Column(
            name = "fully_dispensed_at"
    )
    private LocalDateTime fullyDispensedAt;

    /**
     * Eliminación lógica.
     */
    @Column(nullable = false)
    private Boolean active;

    /**
     * Medicamentos incluidos en la receta.
     *
     * CascadeType.ALL permite guardar la receta
     * y sus medicamentos en la misma transacción.
     */
    @OneToMany(
            mappedBy = "prescription",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<PrescriptionItem> items =
            new ArrayList<>();

    /**
     * Control de concurrencia optimista.
     */
    @Version
    @Column(nullable = false)
    private Long version;
}