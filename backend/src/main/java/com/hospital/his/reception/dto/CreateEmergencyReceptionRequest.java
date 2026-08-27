package com.hospital.his.reception.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEmergencyReceptionRequest {

    private String patientName;

    private String patientDpi;

    private Long branchId;

    private String emergencyNote;
}