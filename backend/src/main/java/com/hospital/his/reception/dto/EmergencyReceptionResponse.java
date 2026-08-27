package com.hospital.his.reception.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyReceptionResponse {

    private Long emergencyReceptionId;

    private Long patientUserId;

    private Boolean registeredPatient;

    private String patientName;

    private String patientDpi;

    private String branch;

    private String priority;

    private String status;

    private String arrivalTime;

    private String emergencyNote;

    private String message;
}