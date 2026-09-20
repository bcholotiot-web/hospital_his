package com.hospital.his.prescriptions.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionResponse {

    private Long prescriptionId;

    private String prescriptionNumber;

    private Long appointmentId;

    private Long medicalConsultationId;

    private Long patientUserId;

    private String patientName;

    private String patientDpi;

    private Long doctorUserId;

    private String doctorName;

    private String branch;

    private String status;

    private String notes;

    private String issuedAt;

    private String updatedAt;

    private String fullyDispensedAt;

    private Integer totalMedications;

    private Integer fullyDispensedMedications;

    private Boolean allMedicationsDispensed;

    /**
     * Indica si la receta aún se encuentra dentro
     * de sus siete días de vigencia.
     */
    private Boolean valid;

    /**
     * Cantidad de días transcurridos desde la emisión.
     */
    private Long elapsedDays;

    private Boolean active;

    private List<PrescriptionItemResponse> items;

    private String message;
}