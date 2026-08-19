package com.hospital.his.appointments.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorResponse {
    private Long id;
    private String fullName;
    private String specialty;
    private String branch;
}