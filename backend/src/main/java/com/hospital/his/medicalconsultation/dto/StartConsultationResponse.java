package com.hospital.his.medicalconsultation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartConsultationResponse {

    private Long appointmentId;

    private Long consultationId;

    private String patientName;

    private String appointmentStatus;

    private String consultationStatus;

    private String startedAt;

    private String message;
}