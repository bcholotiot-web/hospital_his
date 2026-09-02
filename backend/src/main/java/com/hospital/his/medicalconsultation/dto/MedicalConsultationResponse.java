package com.hospital.his.medicalconsultation.dto;

import com.hospital.his.vitalsigns.dto.VitalSignsResponse;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/*ESTE MODULO NOS ESTARA PERMITIENDO VER UNA SOLA PANTALLA
Contexto del paciente
Datos de la cita
Signos vitales
Alertas clínicas
Formulario de consulta
* */
public class MedicalConsultationResponse {

    private Long consultationId;

    private Long appointmentId;

    private String patientName;

    private String patientDpi;

    private String doctorName;

    private String specialty;

    private String branch;

    private String priority;

    private String appointmentStatus;

    private String consultationStatus;

    private String appointmentDateTime;

    private String visitReason;

    private String clinicalFindings;

    private String icd10Code;

    private String icd10Description;

    private String diagnosis;

    private String treatmentPlan;

    private String additionalNotes;

    private String startedAt;

    private String updatedAt;

    private String finishedAt;

    private String careClosedAt;

    private VitalSignsResponse vitalSigns;

    private String message;
}