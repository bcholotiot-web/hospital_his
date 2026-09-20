package com.hospital.his.pharmacy.dto;

import lombok.*;

//Buscar recetas

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionDispensationSearchResponse {

    private Long prescriptionId;

    private String prescriptionNumber;

    private Long medicalConsultationId;

    private String patientName;

    private String patientDpi;

    private String doctorName;

    private String branch;

    private String status;

    private String issuedAt;

    private Long elapsedDays;

    private Boolean valid;

    private Integer totalMedications;

    private Integer pendingMedications;

    private Boolean canDispense;

    private String message;
}