package com.hospital.his.pharmacy.dto;

import lombok.*;

import java.util.List;

//Respuesta detallada de la receta
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionDispensationDetailResponse {

    private Long prescriptionId;

    private String prescriptionNumber;

    private Long medicalConsultationId;

    private String patientName;

    private String patientDpi;

    private String doctorName;

    private String prescriptionStatus;

    private String issuedAt;

    private Long elapsedDays;

    private Long remainingValidityDays;

    private Boolean valid;

    private String pharmacyBranch;

    private Integer totalMedications;

    private Integer pendingMedications;

    private Boolean canDispense;

    private List<PrescriptionDispensationItemResponse>
            items;

    private String message;
}