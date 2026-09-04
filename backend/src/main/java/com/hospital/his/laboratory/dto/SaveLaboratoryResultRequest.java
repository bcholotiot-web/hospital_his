package com.hospital.his.laboratory.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveLaboratoryResultRequest {

    private String resultValue;

    private String unit;

    private LocalDateTime resultDate;

    private Boolean outOfRange;

    private String notes;
}