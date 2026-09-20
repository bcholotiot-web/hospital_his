package com.hospital.his.prescriptions.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionSummaryResponse {

    private Long prescriptionId;

    private String prescriptionNumber;

    private Long appointmentId;

    private Long medicalConsultationId;

    private String patientName;

    private String patientDpi;

    private String doctorName;

    private String branch;

    private String status;

    private String issuedAt;

    private Integer totalMedications;

    private Boolean valid;

    private Long elapsedDays;

    private Boolean active;

    private String message;
}