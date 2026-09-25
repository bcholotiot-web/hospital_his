package com.hospital.his.medicalconsultation.dto;

import com.hospital.his.vitalsigns.dto.VitalSignsResponse;
import com.hospital.his.appointments.dto.AppointmentDocumentResponse;

import java.util.List;
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

    /**
     * Indica si el médico puede generar una orden
     * de laboratorio desde esta consulta.
     */
    private Boolean canCreateLaboratoryOrder;

    /**
     * Indica si ya existe una orden activa.
     */
    private Boolean hasActiveLaboratoryOrder;

    private Boolean canCreatePrescription;
    private Boolean hasActivePrescription;

    /**
     * Indica si el paciente adjuntó al menos
     * un documento en la cita.
     */
    private Boolean hasAppointmentDocuments;

    /**
     * Cantidad de documentos activos asociados.
     */
    private Long appointmentDocumentCount;

    /**
     * Metadatos de documentos disponibles para
     * que el médico pueda visualizarlos.
     *
     * El contenido binario no se incluye aquí.
     */
    private List<AppointmentDocumentResponse>
            appointmentDocuments;
}