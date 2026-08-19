package com.hospital.his.appointments.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {

    private Long id;
    private String patientName;
    private String doctorName;
    private String branch;
    private String specialty;
    private String appointmentDateTime;
    private String reason;
    private String status;
}