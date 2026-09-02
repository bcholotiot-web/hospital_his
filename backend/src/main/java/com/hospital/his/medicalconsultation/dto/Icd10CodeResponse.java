package com.hospital.his.medicalconsultation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Icd10CodeResponse {

    private Long id;

    private String code;

    private String description;
} 