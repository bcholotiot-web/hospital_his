package com.hospital.his.appointments.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAppointmentRequest {

    private Long patientUserId;
    private Long doctorUserId;
    private Long branchId;
    private Long specialtyId;
    private String appointmentDateTime;
    private String reason;
}