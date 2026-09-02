package com.hospital.his.medicalconsultation.entity;

import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/*Guardaremos esta información
Motivo de visita
Hallazgos clínicos
Código CIE-10
Descripción CIE-10
Diagnóstico
Plan de tratamiento
Notas adicionales
Estado de consulta
Fecha de inicio
Fecha de actualización
Fecha de finalización clínica
Fecha de cierre de atención
.*/
@Entity
@Table(name = "medical_consultations",uniqueConstraints = {
        @UniqueConstraint(name = "uk_medical_consultation_appointment",columnNames = "appointment_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalConsultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Una cita solo puede tener una consulta médica.
     */
    @OneToOne
    @JoinColumn(name = "appointment_id",nullable = false,unique = true)
    private Appointment appointment;

    /*
     * El médico se obtiene de la cita y se valida
     * contra el usuario autenticado.
     */
    @ManyToOne
    @JoinColumn(name = "doctor_user_id",nullable = false)
    private User doctor;

    @Column(name = "visit_reason",length = 1000)
    private String visitReason;

    @Column(
            name = "clinical_findings",
            length = 4000
    )
    private String clinicalFindings;

    @Column(
            name = "icd10_code",
            length = 20
    )
    private String icd10Code;

    @Column(
            name = "icd10_description",
            length = 500
    )
    private String icd10Description;

    @Column(
            length = 4000
    )
    private String diagnosis;

    @Column(
            name = "treatment_plan",
            length = 4000
    )
    private String treatmentPlan;

    @Column(
            name = "additional_notes",
            length = 4000
    )
    private String additionalNotes;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private MedicalConsultationStatus status;

    @Column(
            name = "started_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime startedAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @Column(
            name = "finished_at"
    )
    private LocalDateTime finishedAt;

    @Column(
            name = "care_closed_at"
    )
    private LocalDateTime careClosedAt;

    @Version
    @Column(nullable = false)
    private Long version;
}