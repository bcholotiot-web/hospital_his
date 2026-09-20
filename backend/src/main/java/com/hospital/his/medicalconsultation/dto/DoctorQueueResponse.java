package com.hospital.his.medicalconsultation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorQueueResponse {

    private Long appointmentId;

    private Long consultationId;

    private String patientName;

    private String patientDpi;

    private String specialty;

    private String branch;

    private String priority;

    private String appointmentStatus;

    private String consultationStatus;

    private String appointmentDateTime;

    private String arrivalTime;

    private Boolean emergency;

    private Boolean canStartConsultation;

    private Boolean canOpenConsultation;

    private Boolean canFinishCare;

    private Boolean canMarkNoShow;

    private Boolean canGenerateLabOrder;

    private Boolean canGeneratePrescription;

    private Boolean canScheduleFollowUp;
    /**
     * Indica si la consulta ya fue evaluada y todavía
     * no tiene una orden activa de laboratorio.
     */
    private Boolean canCreateLaboratoryOrder;
    private Boolean hasActiveLaboratoryOrder;

    /**
     * Indica si el médico puede generar una receta
     * para la consulta finalizada.
     */
    private Boolean canCreatePrescription;

    /**
     * Indica si la consulta ya tiene una receta activa.
     */
    private Boolean hasActivePrescription;

}