package com.hospital.his.reception.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReassignDoctorRequest {

    private Long newDoctorId;

    private String note;
}