package com.hospital.his.reception.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceptionDoctorResponse {

    private Long id;

    private String fullName;

    private String specialty;

    private String branch;

    private Boolean available;
}