package com.hospital.his.reception.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceptionAppointmentResponse {
    private Long appointmentId;
    private String patientName;
    private String patientDpi;
    private String status;
    private String priority;
    private String specialty;
    private String branch;
    private String doctorName;
    private String appointmentDateTime;
    private String reason;
    private String arrivalTime;
    private Boolean arrivalRegistered;
    private Boolean canRegisterArrival;
    private String message;
}