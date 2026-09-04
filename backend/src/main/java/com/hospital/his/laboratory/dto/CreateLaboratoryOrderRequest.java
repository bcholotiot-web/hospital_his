package com.hospital.his.laboratory.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLaboratoryOrderRequest {

    private Long appointmentId;

    private List<Long> laboratoryTestIds;

    private Boolean externalOrder;

    private String notes;
}